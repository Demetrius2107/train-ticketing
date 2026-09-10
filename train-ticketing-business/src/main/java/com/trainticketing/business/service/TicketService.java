package com.trainticketing.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.DailyTrain;
import com.trainticketing.business.domain.Train;
import com.trainticketing.business.domain.TrainStation;
import com.trainticketing.business.mapper.DailyTrainMapper;
import com.trainticketing.business.mapper.DailyTrainSeatMapper;
import com.trainticketing.business.mapper.TrainMapper;
import com.trainticketing.business.mapper.TrainStationMapper;
import com.trainticketing.business.resp.SeatRemainingBatchResp;
import com.trainticketing.business.resp.SeatRemainingResp;
import com.trainticketing.business.resp.TrainTicketResp;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <p>Title: TicketService</p>
 * <p>Description: 余票查询服务（核心业务）：基于区间占用模型统计可售座位，支持单次区间查询与车次聚合查询</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
@Service
public class TicketService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketService.class);

    @Resource
    private DailyTrainMapper dailyTrainMapper;

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    @Resource
    private TrainStationMapper trainStationMapper;

    @Resource
    private TrainMapper trainMapper;

    @Resource
    private TicketCacheService ticketCacheService;

    /**
     * 查询指定排班某区间的余票（区间占用模型）。
     * 业务规则：出发/到达站必须是该车次经停站，且出发站序必须小于到达站序；
     * 余票读 Redis 缓存（路径相邻段最小值，缺失段自动回源 DB），与下单预扣共用
     * 同一数据源，保证查买一致——查询路径不再直接扫座位表，读容量与 MySQL 解耦。
     *
     * @param dailyTrainId    排班ID
     * @param departStationId 出发站id
     * @param arriveStationId 到达站id
     * @return 各座位类型余票
     */
    public List<SeatRemainingResp> queryRemaining(Long dailyTrainId, Long departStationId, Long arriveStationId) {
        DailyTrain dailyTrain = dailyTrainMapper.selectById(dailyTrainId);
        if (ObjectUtil.isNull(dailyTrain)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_DAILY_TRAIN_NOT_EXIST);
        }
        TrainStation depart = trainStationMapper.selectByStation(dailyTrain.getTrainId(), departStationId);
        TrainStation arrive = trainStationMapper.selectByStation(dailyTrain.getTrainId(), arriveStationId);
        if (ObjectUtil.isNull(depart) || ObjectUtil.isNull(arrive)
                || depart.getStationIndex() >= arrive.getStationIndex()) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_STATION_INDEX_INVALID);
        }
        List<String> seatTypes = dailyTrainSeatMapper.selectSeatTypes(dailyTrainId);
        List<SeatRemainingResp> result = new ArrayList<>(seatTypes.size());
        for (String seatType : seatTypes) {
            int remain = ticketCacheService.getRemaining(dailyTrainId, seatType,
                    depart.getStationIndex(), arrive.getStationIndex());
            SeatRemainingResp resp = new SeatRemainingResp();
            resp.setSeatType(seatType);
            resp.setRemainingCount((long) remain);
            result.add(resp);
        }
        return result;
    }

    /**
     * 按出发站/到达站/日期查询车次列表及各自区间余票（用户侧核心查询）。
     * 仅返回当天运行中的排班；每个排班附带经停站序与区间余票。
     * <p>批量组装：车次信息、经停站序、区间余票各一次批量查询，
     * 消除逐排班 selectById/selectByStation/selectRemainingByInterval 的 N+1；
     * 余票语义与详情页 queryRemaining 一致（区间占用模型，NOT EXISTS 重叠判定）。
     *
     * @param fromStationId 出发站id
     * @param toStationId   到达站id
     * @param runDate       运行日期
     * @return 车次余票列表
     */
    public List<TrainTicketResp> queryByStations(Long fromStationId, Long toStationId, LocalDate runDate) {
        List<DailyTrain> dailyList = dailyTrainMapper.selectByStationsAndDate(fromStationId, toStationId, runDate);
        if (CollUtil.isEmpty(dailyList)) {
            return new ArrayList<>();
        }
        List<Long> dailyTrainIds = dailyList.stream().map(DailyTrain::getId).toList();
        List<Long> trainIds = dailyList.stream().map(DailyTrain::getTrainId).distinct().toList();
        Map<Long, Train> trainMap = trainMapper.selectByIds(trainIds).stream()
                .collect(Collectors.toMap(Train::getId, Function.identity()));
        Map<Long, Map<Long, TrainStation>> stationMap = trainStationMapper.selectByTrainIds(trainIds).stream()
                .collect(Collectors.groupingBy(TrainStation::getTrainId,
                        Collectors.toMap(TrainStation::getStationId, Function.identity())));
        Map<Long, List<SeatRemainingResp>> remainingMap = new HashMap<>();
        for (SeatRemainingBatchResp batch : dailyTrainSeatMapper.selectRemainingByIntervalBatch(
                dailyTrainIds, fromStationId, toStationId)) {
            SeatRemainingResp resp = new SeatRemainingResp();
            resp.setSeatType(batch.getSeatType());
            resp.setRemainingCount(batch.getRemainingCount());
            remainingMap.computeIfAbsent(batch.getDailyTrainId(), k -> new ArrayList<>()).add(resp);
        }
        List<TrainTicketResp> respList = new ArrayList<>(dailyList.size());
        for (DailyTrain dailyTrain : dailyList) {
            Train train = trainMap.get(dailyTrain.getTrainId());
            Map<Long, TrainStation> stations = stationMap.getOrDefault(dailyTrain.getTrainId(), Map.of());
            TrainStation depart = stations.get(fromStationId);
            TrainStation arrive = stations.get(toStationId);
            // selectByStationsAndDate 已 join 经停站，正常不会缺；防御脏数据跳过该车次
            if (train == null || depart == null || arrive == null) {
                LOG.warn("车次列表数据不完整，跳过 dailyTrainId={}, trainId={}",
                        dailyTrain.getId(), dailyTrain.getTrainId());
                continue;
            }
            TrainTicketResp resp = BeanUtil.copyProperties(dailyTrain, TrainTicketResp.class);
            resp.setTrainCode(train.getCode());
            resp.setDepartStationId(fromStationId);
            resp.setArriveStationId(toStationId);
            resp.setDepartIndex(depart.getStationIndex());
            resp.setArriveIndex(arrive.getStationIndex());
            resp.setRemainingList(remainingMap.getOrDefault(dailyTrain.getId(), List.of()));
            respList.add(resp);
        }
        return respList;
    }
}
