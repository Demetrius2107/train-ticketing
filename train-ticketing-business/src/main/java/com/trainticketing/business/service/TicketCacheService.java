package com.trainticketing.business.service;

import cn.hutool.core.collection.CollUtil;
import com.trainticketing.business.mapper.DailyTrainSeatMapper;
import com.trainticketing.business.resp.SeatRemainingResp;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * <p>Title: TicketCacheService</p>
 * <p>Description: 余票缓存服务（阶段2 高并发核心）：
 * 余票以 Redis Hash 缓存，key=ticket:remain:{dailyTrainId}:{seatType}，
 * field={i}_{i+1}（相邻站序段），value=该相邻段剩余可售座位数。
 * 一张 A→C 的票同时占用 A-B、B-C 两个相邻段，故下单需对路径上所有相邻段原子扣减，
 * 任一段不足即整体失败；查询 [depart,arrive] 余票 = 路径上各相邻段余票的最小值。
 * 此"相邻子段"模型与 DB 区间占用模型一致，杜绝跨区间重叠超卖。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-17
 */
@Service
public class TicketCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketCacheService.class);

    /** 余票缓存 key 前缀：ticket:remain:{dailyTrainId}:{seatType} */
    private static final String REMAIN_KEY_PREFIX = "ticket:remain:";

    /**
     * Lua 原子多段预扣脚本（相邻子段模型）：
     * KEYS[1] = 余票 hash key；ARGV[1] = 预扣数量 need；ARGV[2..] = 各相邻段 field。
     * 先逐段校验余票存在且 >= need，全部通过后再统一扣减；任一段不足返回 -1。
     * 单条 Lua 在 Redis 中原子执行，杜绝并发超卖与跨区间重叠超卖。
     */
    private static final String DECR_SCRIPT =
        "local need = tonumber(ARGV[1]) "
        + "local fields = {} "
        + "for i = 2, #ARGV do "
        + "  local f = ARGV[i] "
        + "  local remain = redis.call('HGET', KEYS[1], f) "
        + "  if remain == false then return -1 end "
        + "  if tonumber(remain) < need then return -1 end "
        + "  fields[i] = f "
        + "end "
        + "for i = 2, #ARGV do "
        + "  redis.call('HINCRBY', KEYS[1], ARGV[i], -need) "
        + "end "
        + "return 1";

    /** Lua 多段回补脚本：ARGV[1]=回补数量，ARGV[2..]=各相邻段 field */
    private static final String INCR_SCRIPT =
        "local count = tonumber(ARGV[1]) "
        + "for i = 2, #ARGV do "
        + "  redis.call('HINCRBY', KEYS[1], ARGV[i], count) "
        + "end "
        + "return 1";

    private final DefaultRedisScript<Long> decrScript = new DefaultRedisScript<>(DECR_SCRIPT, Long.class);
    private final DefaultRedisScript<Long> incrScript = new DefaultRedisScript<>(INCR_SCRIPT, Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    /**
     * 构建余票缓存 key
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @return key
     */
    private String remainKey(Long dailyTrainId, String seatType) {
        return REMAIN_KEY_PREFIX + dailyTrainId + ":" + seatType;
    }

    /**
     * 构建相邻段 field：站序 i → i+1
     *
     * @param segIndex 段起点站序
     * @return field
     */
    private String remainField(int segIndex) {
        return segIndex + "_" + (segIndex + 1);
    }

    /**
     * 展开区间 [departIndex, arriveIndex] 为相邻段起点列表。
     * 例如 depart=1, arrive=3 → [1, 2]（对应段 1_2、2_3）。
     *
     * @param departIndex 出发站序
     * @param arriveIndex 到达站序
     * @return 相邻段起点站序列表
     */
    private List<Integer> expandSegments(int departIndex, int arriveIndex) {
        List<Integer> segments = new ArrayList<>(arriveIndex - departIndex);
        for (int i = departIndex; i < arriveIndex; i++) {
            segments.add(i);
        }
        return segments;
    }

    /**
     * 获取区间余票（懒加载）：缓存未命中时查 DB 回填。
     * 相邻子段模型下，[depart,arrive] 余票 = 路径上各相邻段余票的最小值。
     * 单段缓存缺失时按 DB 该相邻段可售数回填（DB 区间占用模型保证正确性）。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param departIndex  出发站序
     * @param arriveIndex  到达站序
     * @return 余票数（缓存缺失且 DB 无数据返回 0）
     */
    public int getRemaining(Long dailyTrainId, String seatType, Integer departIndex, Integer arriveIndex) {
        String key = remainKey(dailyTrainId, seatType);
        List<Integer> segments = expandSegments(departIndex, arriveIndex);
        int min = Integer.MAX_VALUE;
        boolean anyMissing = false;
        for (int seg : segments) {
            String field = remainField(seg);
            String cached = (String) stringRedisTemplate.opsForHash().get(key, field);
            if (cached == null) {
                anyMissing = true;
                break;
            }
            min = Math.min(min, Integer.parseInt(cached));
        }
        if (!anyMissing) {
            return min;
        }
        // 懒加载回填：以 DB 区间余票为准，回填路径上每个缺失的相邻段
        List<SeatRemainingResp> remaining = dailyTrainSeatMapper.selectRemainingByInterval(
            dailyTrainId, departIndex, arriveIndex);
        int dbCount = 0;
        for (SeatRemainingResp resp : remaining) {
            if (seatType.equals(resp.getSeatType())) {
                dbCount = resp.getRemainingCount().intValue();
                break;
            }
        }
        // DB 返回的是整段可售票数（min 语义），统一回填到路径各相邻段
        for (int seg : segments) {
            stringRedisTemplate.opsForHash().put(key, remainField(seg), String.valueOf(dbCount));
        }
        LOG.info("余票缓存回填 key={}, [{}-{}], count={}", key, departIndex, arriveIndex, dbCount);
        return dbCount;
    }

    /**
     * 预热排班某相邻段余票缓存：将该排班某座位类型的相邻段余票写入 Redis。
     * 座位生成后无订单占用，各相邻段初始余票 = 该座位类型座位总数。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param segIndex     相邻段起点站序（段为 segIndex → segIndex+1）
     * @param count        初始余票数
     */
    public void initRemaining(Long dailyTrainId, String seatType, int segIndex, int count) {
        String key = remainKey(dailyTrainId, seatType);
        stringRedisTemplate.opsForHash().put(key, remainField(segIndex), String.valueOf(count));
    }

    /**
     * 删除排班余票缓存（排班销毁/数据重置时用）
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     */
    public void deleteRemaining(Long dailyTrainId, String seatType) {
        stringRedisTemplate.delete(remainKey(dailyTrainId, seatType));
    }

    /**
     * Lua 原子预扣区间余票（下单调用，按乘车人数扣减）。
     * 对路径上所有相邻段逐段校验并统一扣减，任一段不足或未初始化返回 -1（防超卖）。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param departIndex  出发站序
     * @param arriveIndex  到达站序
     * @param need         预扣数量（乘车人数）
     * @return 1 成功；-1 失败（余票不足/未初始化）
     */
    public long decrRemaining(Long dailyTrainId, String seatType, Integer departIndex, Integer arriveIndex, int need) {
        try {
            List<Integer> segments = expandSegments(departIndex, arriveIndex);
            List<String> argv = new ArrayList<>(segments.size() + 1);
            argv.add(String.valueOf(need));
            for (int seg : segments) {
                argv.add(remainField(seg));
            }
            Long ret = stringRedisTemplate.execute(decrScript,
                Arrays.asList(remainKey(dailyTrainId, seatType)),
                argv.toArray());
            return ret == null ? -1 : ret;
        } catch (Exception e) {
            LOG.error("Lua 预扣余票失败 dailyTrainId={}, seatType={}, [{}-{}], error={}",
                dailyTrainId, seatType, departIndex, arriveIndex, e.getMessage());
            return -1;
        }
    }

    /**
     * Lua 原子回补区间余票（订单取消/超时/退款释放占用时调用，按释放数量回补路径各段）。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param departIndex  出发站序
     * @param arriveIndex  到达站序
     * @param count        回补数量（释放票数）
     */
    public void incrRemaining(Long dailyTrainId, String seatType, Integer departIndex, Integer arriveIndex, int count) {
        try {
            List<Integer> segments = expandSegments(departIndex, arriveIndex);
            List<String> argv = new ArrayList<>(segments.size() + 1);
            argv.add(String.valueOf(count));
            for (int seg : segments) {
                argv.add(remainField(seg));
            }
            stringRedisTemplate.execute(incrScript,
                Arrays.asList(remainKey(dailyTrainId, seatType)),
                argv.toArray());
        } catch (Exception e) {
            LOG.error("Lua 回补余票失败 dailyTrainId={}, seatType={}, [{}-{}], error={}",
                dailyTrainId, seatType, departIndex, arriveIndex, e.getMessage());
        }
    }

    /**
     * 对账：以 DB 区间占用模型为准，重建某排班某座位类型的所有相邻段余票缓存。
     * <p>流程：先删除该 key 旧缓存，再逐个相邻段查 DB selectRemainingByInterval(dailyTrainId, i, i+1)
     * 得到该相邻段可售座位数（DB 区间重叠 LEFT JOIN 保证正确性），写入缓存。
     * <p>兜底场景：缓存丢失、漂移、或服务重启后缓存未预热时由定时任务或手动触发修复。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param segIndexes   相邻段起点站序列表（升序，段为 i→i+1）
     * @return 重建的相邻段数
     */
    public int reconcileRemaining(Long dailyTrainId, String seatType, List<Integer> segIndexes) {
        if (CollUtil.isEmpty(segIndexes)) {
            return 0;
        }
        String key = remainKey(dailyTrainId, seatType);
        // 先删旧缓存，避免残留脏字段
        stringRedisTemplate.delete(key);
        for (Integer seg : segIndexes) {
            // 相邻段 [seg, seg+1] 的可售票数 = DB 该区间未被占用的座位数
            List<SeatRemainingResp> remaining = dailyTrainSeatMapper.selectRemainingByInterval(
                dailyTrainId, seg, seg + 1);
            int count = 0;
            for (SeatRemainingResp resp : remaining) {
                if (seatType.equals(resp.getSeatType())) {
                    count = resp.getRemainingCount().intValue();
                    break;
                }
            }
            stringRedisTemplate.opsForHash().put(key, remainField(seg), String.valueOf(count));
        }
        LOG.info("余票缓存对账重建 dailyTrainId={}, seatType={}, 段数={}", dailyTrainId, seatType, segIndexes.size());
        return segIndexes.size();
    }
}
