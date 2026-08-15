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
import com.trainticketing.business.resp.SeatRemainingResp;
import com.trainticketing.business.resp.TrainTicketResp;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <p>Title: TicketService</p>
 * <p>Description: 余票查询服务（核心业务）：基于区间占用模型统计可售座位，支持单次区间查询与车次聚合查询</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
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

    /**
     * 查询指定排班某区间的余票（区间占用模型）。
     * 业务规则：出发/到达站必须是该车次经停站，且出发站序必须小于到达站序；
     * 余票统计见 DailyTrainSeatMapper.selectRemainingByInterval。
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
        return dailyTrainSeatMapper.selectRemainingByInterval(dailyTrainId,
            depart.getStationIndex(), arrive.getStationIndex());
    }

    /**
     * 按出发站/到达站/日期查询车次列表及各自区间余票（用户侧核心查询）。
     * 仅返回当天运行中的排班；每个排班附带经停站序与区间余票。
     *
     * @param fromStationId 出发站id
     * @param toStationId   到达站id
     * @param runDate       运行日期
     * @return 车次余票列表
     */
    public List<TrainTicketResp> queryByStations(Long fromStationId, Long toStationId, LocalDate runDate) {
        List<DailyTrain> dailyList = dailyTrainMapper.selectByStationsAndDate(fromStationId, toStationId, runDate);
        List<TrainTicketResp> respList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dailyList)) {
            for (DailyTrain dailyTrain : dailyList) {
                respList.add(buildTicketResp(dailyTrain, fromStationId, toStationId));
            }
        }
        return respList;
    }

    /**
     * 组装单个车次的余票响应（车次信息 + 区间余票）
     *
     * @param dailyTrain    排班
     * @param fromStationId 出发站id
     * @param toStationId   到达站id
     * @return 车次余票响应
     */
    private TrainTicketResp buildTicketResp(DailyTrain dailyTrain, Long fromStationId, Long toStationId) {
        Train train = trainMapper.selectById(dailyTrain.getTrainId());
        TrainStation depart = trainStationMapper.selectByStation(dailyTrain.getTrainId(), fromStationId);
        TrainStation arrive = trainStationMapper.selectByStation(dailyTrain.getTrainId(), toStationId);
        TrainTicketResp resp = BeanUtil.copyProperties(dailyTrain, TrainTicketResp.class);
        resp.setTrainCode(train.getCode());
        resp.setDepartStationId(fromStationId);
        resp.setArriveStationId(toStationId);
        resp.setDepartIndex(depart.getStationIndex());
        resp.setArriveIndex(arrive.getStationIndex());
        resp.setRemainingList(dailyTrainSeatMapper.selectRemainingByInterval(dailyTrain.getId(),
            depart.getStationIndex(), arrive.getStationIndex()));
        return resp;
    }
}
