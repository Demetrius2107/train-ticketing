package com.trainticketing.business.service;

import com.trainticketing.business.mapper.DailyTrainSeatMapper;
import com.trainticketing.business.resp.SeatRemainingResp;
import jakarta.annotation.Resource;
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
 * field={departIndex}_{arriveIndex}（区间），value=该区间余票数。
 * 查询懒加载回填；下单用 Lua 原子预扣防超卖；取消/超时回补。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@Service
public class TicketCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketCacheService.class);

    /** 余票缓存 key 前缀：ticket:remain:{dailyTrainId}:{seatType} */
    private static final String REMAIN_KEY_PREFIX = "ticket:remain:";

    /**
     * Lua 原子预扣脚本：
     * 若区间余票存在且 >= 预扣数量，则扣减并返回剩余值；否则返回 -1（无余票/未初始化）。
     * 单条 Lua 在 Redis 中原子执行，杜绝并发超卖。
     */
    private static final String DECR_SCRIPT =
        "local remain = redis.call('HGET', KEYS[1], ARGV[1]) "
        + "if remain == false then return -1 end "
        + "local n = tonumber(remain) "
        + "local need = tonumber(ARGV[2]) "
        + "if n < need then return -1 end "
        + "redis.call('HINCRBY', KEYS[1], ARGV[1], -need) "
        + "return n - need";

    /** Lua 回补脚本：区间余票 +1（取消/超时释放占用） */
    private static final String INCR_SCRIPT =
        "redis.call('HINCRBY', KEYS[1], ARGV[1], 1) "
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
     * 构建区间 field
     *
     * @param departIndex 出发站序
     * @param arriveIndex 到达站序
     * @return field
     */
    private String remainField(Integer departIndex, Integer arriveIndex) {
        return departIndex + "_" + arriveIndex;
    }

    /**
     * 获取区间余票（懒加载）：缓存未命中时查 DB 回填。
     * 回填值 = 当前 DB 中该区间可售座位数（区间占用模型）。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param departIndex  出发站序
     * @param arriveIndex  到达站序
     * @return 余票数（缓存缺失且 DB 无数据返回 0）
     */
    public int getRemaining(Long dailyTrainId, String seatType, Integer departIndex, Integer arriveIndex) {
        String key = remainKey(dailyTrainId, seatType);
        String field = remainField(departIndex, arriveIndex);
        String cached = (String) stringRedisTemplate.opsForHash().get(key, field);
        if (cached != null) {
            return Integer.parseInt(cached);
        }
        // 懒加载回填：查 DB 该区间余票
        List<SeatRemainingResp> remaining = dailyTrainSeatMapper.selectRemainingByInterval(
            dailyTrainId, departIndex, arriveIndex);
        int count = 0;
        for (SeatRemainingResp resp : remaining) {
            if (seatType.equals(resp.getSeatType())) {
                count = resp.getRemainingCount().intValue();
                break;
            }
        }
        stringRedisTemplate.opsForHash().put(key, field, String.valueOf(count));
        LOG.info("余票缓存回填 key={}, field={}, count={}", key, field, count);
        return count;
    }

    /**
     * 预热排班余票缓存：将该排班各座位类型的区间余票写入 Redis（排班座位生成后调用）。
     * 以 DB 当前区间余票为准（生成后无订单占用，即各区间=该座位类型座位总数）。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param departIndex  出发站序
     * @param arriveIndex  到达站序
     * @param count        初始余票数
     */
    public void initRemaining(Long dailyTrainId, String seatType, Integer departIndex, Integer arriveIndex, int count) {
        String key = remainKey(dailyTrainId, seatType);
        stringRedisTemplate.opsForHash().put(key, remainField(departIndex, arriveIndex), String.valueOf(count));
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
     * 无余票或区间未初始化时返回 -1，由调用方判定下单失败（防超卖）。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param departIndex  出发站序
     * @param arriveIndex  到达站序
     * @param need         预扣数量（乘车人数）
     * @return 预扣后的剩余余票；-1 表示失败（余票不足）
     */
    public long decrRemaining(Long dailyTrainId, String seatType, Integer departIndex, Integer arriveIndex, int need) {
        try {
            Long remain = stringRedisTemplate.execute(decrScript,
                Arrays.asList(remainKey(dailyTrainId, seatType)),
                remainField(departIndex, arriveIndex), String.valueOf(need));
            return remain == null ? -1 : remain;
        } catch (Exception e) {
            LOG.error("Lua 预扣余票失败 key={}, field={}, error={}",
                remainKey(dailyTrainId, seatType), remainField(departIndex, arriveIndex), e.getMessage());
            return -1;
        }
    }

    /**
     * Lua 原子回补区间余票（订单取消/超时释放占用时调用，按释放数量回补）。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param departIndex  出发站序
     * @param arriveIndex  到达站序
     * @param count        回补数量（释放票数）
     */
    public void incrRemaining(Long dailyTrainId, String seatType, Integer departIndex, Integer arriveIndex, int count) {
        try {
            String field = remainField(departIndex, arriveIndex);
            stringRedisTemplate.execute(incrScript,
                Arrays.asList(remainKey(dailyTrainId, seatType)), field, String.valueOf(count));
        } catch (Exception e) {
            LOG.error("Lua 回补余票失败 key={}, field={}, error={}",
                remainKey(dailyTrainId, seatType), remainField(departIndex, arriveIndex), e.getMessage());
        }
    }
}
