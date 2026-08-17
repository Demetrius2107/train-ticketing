package com.trainticketing.business.service;

import cn.hutool.core.collection.CollUtil;
import com.trainticketing.business.domain.DailyTrain;
import com.trainticketing.business.domain.DailyTrainSeat;
import com.trainticketing.business.domain.TrainStation;
import com.trainticketing.business.mapper.DailyTrainMapper;
import com.trainticketing.business.mapper.DailyTrainSeatMapper;
import com.trainticketing.business.mapper.TrainStationMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * <p>Title: TicketReconcileService</p>
 * <p>Description: 余票缓存对账服务（阶段2.1 兜底）：
 * 定时以 DB 区间占用模型为准重建 Redis 余票缓存，修正缓存丢失/漂移/未预热。
 * 缓存-DB 一致性有三道前置防线（Redisson 锁 + Lua 预扣 + afterCommit 回补），
 * 对账是最后兜底，防止极端异常（进程崩溃、Redis 故障恢复）导致长期不一致。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-17
 * @updateTime 2026-08-17
 */
@Service
public class TicketReconcileService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketReconcileService.class);

    @Resource
    private DailyTrainMapper dailyTrainMapper;

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    @Resource
    private TrainStationMapper trainStationMapper;

    @Resource
    private TicketCacheService ticketCacheService;

    /**
     * 定时对账：每小时整点执行，重建所有运行中排班的余票缓存。
     * cron = 秒 分 时 日 月 周（本地时区）。固定速率场景下避免与下单争抢，
     * 重建是 delete+put 原子粒度到单 key，对查询有短暂 miss 但不会错卖（Lua 预扣 + DB 行锁兜底）。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void reconcileAll() {
        List<DailyTrain> runningList = dailyTrainMapper.selectRunningForReconcile();
        if (CollUtil.isEmpty(runningList)) {
            LOG.info("余票对账：无运行中排班，跳过");
            return;
        }
        LOG.info("余票对账开始，运行中排班数={}", runningList.size());
        int reconciled = 0;
        for (DailyTrain dailyTrain : runningList) {
            try {
                reconciled += reconcileDailyTrain(dailyTrain);
            } catch (Exception e) {
                LOG.error("余票对账单排班失败 dailyTrainId={}, error={}",
                    dailyTrain.getId(), e.getMessage());
            }
        }
        LOG.info("余票对账结束，重建排班数={}/{}", reconciled, runningList.size());
    }

    /**
     * 手动触发对账单个排班（运维/排障用，由 Controller 调用）。
     *
     * @param dailyTrainId 排班ID
     * @return 重建的座位类型数；排班无当日座位返回 0
     */
    public int reconcileDailyTrain(Long dailyTrainId) {
        DailyTrain dailyTrain = dailyTrainMapper.selectById(dailyTrainId);
        if (dailyTrain == null) {
            return 0;
        }
        return reconcileDailyTrain(dailyTrain);
    }

    /**
     * 对账单个排班：取经停站站序→相邻段；取当日座位涉及的座位类型；
     * 对每个座位类型调用 TicketCacheService 按 DB 重建相邻段缓存。
     *
     * @param dailyTrain 排班
     * @return 重建的座位类型数
     */
    private int reconcileDailyTrain(DailyTrain dailyTrain) {
        Long dailyTrainId = dailyTrain.getId();
        // 经停站按站序升序，取相邻段起点
        List<TrainStation> stationList = trainStationMapper.selectByTrainId(dailyTrain.getTrainId());
        if (CollUtil.isEmpty(stationList) || stationList.size() < 2) {
            LOG.warn("余票对账：排班 {} 经停站不足，跳过", dailyTrainId);
            return 0;
        }
        List<Integer> segIndexes = new ArrayList<>(stationList.size() - 1);
        for (int i = 0; i < stationList.size() - 1; i++) {
            segIndexes.add(stationList.get(i).getStationIndex());
        }
        // 当日座位涉及的座位类型（去重保序）
        List<DailyTrainSeat> seats = dailyTrainSeatMapper.selectByDailyTrainId(dailyTrainId);
        if (CollUtil.isEmpty(seats)) {
            LOG.info("余票对账：排班 {} 无当日座位，跳过", dailyTrainId);
            return 0;
        }
        Set<String> seatTypes = new LinkedHashSet<>();
        for (DailyTrainSeat seat : seats) {
            seatTypes.add(seat.getSeatType());
        }
        int count = 0;
        for (String seatType : seatTypes) {
            ticketCacheService.reconcileRemaining(dailyTrainId, seatType, segIndexes);
            count++;
        }
        return count;
    }
}
