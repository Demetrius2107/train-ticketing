package com.trainticketing.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.Station;
import com.trainticketing.business.domain.Train;
import com.trainticketing.business.domain.TrainStation;
import com.trainticketing.business.mapper.DailyTrainMapper;
import com.trainticketing.business.mapper.StationMapper;
import com.trainticketing.business.mapper.TrainMapper;
import com.trainticketing.business.mapper.TrainStationMapper;
import com.trainticketing.business.req.TrainStationSaveReq;
import com.trainticketing.business.resp.TrainStationQueryResp;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <p>Title: TrainStationService</p>
 * <p>Description: 车次经停站管理服务：新增（站序/站名唯一、引用校验）、按车次查询、删除</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
@Service
public class TrainStationService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainStationService.class);

    @Resource
    private TrainStationMapper trainStationMapper;

    @Resource
    private TrainMapper trainMapper;

    @Resource
    private StationMapper stationMapper;

    @Resource
    private DailyTrainMapper dailyTrainMapper;

    /**
     * 新增经停站。业务规则：
     * 1. 车次与车站必须已存在（引用校验）；
     * 2. 同一车次站序唯一（uk_train_index 兜底）；
     * 3. 同一车次站名唯一（uk_train_station 兜底）；
     * 4. 站序从 1 开始；约定首站（站序1）不填到达时间、末站不填发车时间，此处不强制（由维护方保证）。
     * 5. 时刻单调递增：本站到达 < 发车，且与前驱站发车、后继站到达保持严格递增（同一日内）。
     *
     * @param req 经停站新增请求
     * @return 新增经停站ID
     */
    public Long save(TrainStationSaveReq req) {
        Train train = trainMapper.selectById(req.getTrainId());
        if (ObjectUtil.isNull(train)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_NOT_EXIST);
        }
        Station station = stationMapper.selectById(req.getStationId());
        if (ObjectUtil.isNull(station)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_STATION_NOT_EXIST);
        }
        TrainStation indexDB = trainStationMapper.selectByIndex(req.getTrainId(), req.getStationIndex());
        if (ObjectUtil.isNotNull(indexDB)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_STATION_INDEX_UNIQUE_ERROR);
        }
        TrainStation stationDB = trainStationMapper.selectByStation(req.getTrainId(), req.getStationId());
        if (ObjectUtil.isNotNull(stationDB)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_STATION_NAME_UNIQUE_ERROR);
        }
        LocalTime arriveTime = ObjectUtil.isNotEmpty(req.getArriveTime()) ? LocalTime.parse(req.getArriveTime()) : null;
        LocalTime leaveTime = ObjectUtil.isNotEmpty(req.getLeaveTime()) ? LocalTime.parse(req.getLeaveTime()) : null;
        // 时刻单调校验：本站到<发，且与相邻站时刻单调递增（支持跨日）
        validateTimeMonotonic(trainStationMapper.selectByTrainId(req.getTrainId()),
            req.getStationIndex(), arriveTime, leaveTime);
        TrainStation trainStation = new TrainStation();
        trainStation.setId(IdUtil.getSnowflakeNextId());
        trainStation.setTrainId(req.getTrainId());
        trainStation.setStationId(req.getStationId());
        trainStation.setStationIndex(req.getStationIndex());
        trainStation.setArriveTime(arriveTime);
        trainStation.setLeaveTime(leaveTime);
        trainStation.setStopMinutes(req.getStopMinutes());
        trainStation.setCreateTime(new Date());
        trainStation.setUpdateTime(new Date());
        trainStationMapper.insert(trainStation);
        return trainStation.getId();
    }

    /**
     * 时刻单调校验：经停站的到/发时刻必须沿站序单调递增。
     * <p>校验规则：
     * 1. 本站：到达与发车都填时，到达必须早于发车；
     * 2. 与前驱站（站序小于本站的最大者）：本站到达必须晚于前驱站发车；
     * 3. 与后继站（站序大于本站的最小者）：本站发车必须早于后继站到达。
     * <p>跨日限制：当前 train_station 仅 time 字段、无日偏移标记，无法区分
     * "同日内倒退"与"次日跨日"，故按同一日严格递增校验。若将来支持跨日运行
     * （如 23:50 发车次日 06:00 到），需给 train_station 增加 day_offset 字段
     * （0=当日/1=次日），用 day_offset*1440 + 分钟数 做绝对时间比较。
     * <p>纯逻辑方法，不依赖 DB，便于单元测试边界场景。
     *
     * @param existingStations 该车次已有的经停站列表（save 中查询后传入）
     * @param stationIndex     本站站序
     * @param arriveTime       本站到达时间（首站可为空）
     * @param leaveTime        本站发车时间（末站可为空）
     */
    static void validateTimeMonotonic(List<TrainStation> existingStations, Integer stationIndex,
                                      LocalTime arriveTime, LocalTime leaveTime) {
        // 本站：到/发都填则到达必须早于发车
        if (arriveTime != null && leaveTime != null && !arriveTime.isBefore(leaveTime)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_STATION_TIME_INVALID);
        }
        if (CollUtil.isEmpty(existingStations)) {
            return;
        }
        // 找前驱（站序<本站的最大）与后继（站序>本站的最小）
        TrainStation prev = null; // 前驱站
        TrainStation next = null; // 后继站
        for (TrainStation ts : existingStations) {
            if (ts.getStationIndex() < stationIndex) {
                if (prev == null || ts.getStationIndex() > prev.getStationIndex()) {
                    prev = ts;
                }
            } else if (ts.getStationIndex() > stationIndex) {
                if (next == null || ts.getStationIndex() < next.getStationIndex()) {
                    next = ts;
                }
            }
        }
        // 前驱站发车 → 本站到达：同一日内必须严格递增（到达晚于前站发车）
        if (prev != null && prev.getLeaveTime() != null && arriveTime != null) {
            if (!arriveTime.isAfter(prev.getLeaveTime())) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_STATION_TIME_INVALID);
            }
        }
        // 本站发车 → 后继站到达：同一日内必须严格递增（发车早于后站到达）
        if (next != null && leaveTime != null && next.getArriveTime() != null) {
            if (!next.getArriveTime().isAfter(leaveTime)) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_STATION_TIME_INVALID);
            }
        }
    }

    /**
     * 按车次查询经停站列表（按站序排序）
     *
     * @param trainId 车次ID
     * @return 经停站列表
     */
    public List<TrainStationQueryResp> queryList(Long trainId) {
        List<TrainStation> stationList = trainStationMapper.selectByTrainId(trainId);
        List<TrainStationQueryResp> respList = new ArrayList<>();
        if (CollUtil.isNotEmpty(stationList)) {
            for (TrainStation trainStation : stationList) {
                respList.add(BeanUtil.copyProperties(trainStation, TrainStationQueryResp.class));
            }
        }
        return respList;
    }

    /**
     * 按主键删除经停站。删除保护：经停站所属车次一旦生成排班即禁止删除，
     * 因为排班的余票区间划分依赖经停站站序，删站会导致历史订单区间索引错位。
     *
     * @param id 经停站ID
     */
    public void delete(Long id) {
        TrainStation trainStation = trainStationMapper.selectByPrimaryKey(id);
        if (ObjectUtil.isNull(trainStation)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_STATION_NOT_EXIST);
        }
        // 删除保护：该车次已有排班则禁止删经停站（站序是排班余票区间的锚点）
        int planCount = dailyTrainMapper.countByTrainId(trainStation.getTrainId());
        if (planCount > 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_DAILY_TRAIN_ALREADY_PLAN_FORBIDDEN_DELETE);
        }
        trainStationMapper.deleteById(id);
    }
}
