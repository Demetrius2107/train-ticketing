package com.trainticketing.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.Station;
import com.trainticketing.business.domain.Train;
import com.trainticketing.business.domain.TrainStation;
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

    /**
     * 新增经停站。业务规则：
     * 1. 车次与车站必须已存在（引用校验）；
     * 2. 同一车次站序唯一（uk_train_index 兜底）；
     * 3. 同一车次站名唯一（uk_train_station 兜底）；
     * 4. 站序从 1 开始；约定首站（站序1）不填到达时间、末站不填发车时间，此处不强制（由维护方保证）。
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
        TrainStation trainStation = new TrainStation();
        trainStation.setId(IdUtil.getSnowflakeNextId());
        trainStation.setTrainId(req.getTrainId());
        trainStation.setStationId(req.getStationId());
        trainStation.setStationIndex(req.getStationIndex());
        trainStation.setArriveTime(ObjectUtil.isNotEmpty(req.getArriveTime()) ? LocalTime.parse(req.getArriveTime()) : null);
        trainStation.setLeaveTime(ObjectUtil.isNotEmpty(req.getLeaveTime()) ? LocalTime.parse(req.getLeaveTime()) : null);
        trainStation.setStopMinutes(req.getStopMinutes());
        trainStation.setCreateTime(new Date());
        trainStation.setUpdateTime(new Date());
        trainStationMapper.insert(trainStation);
        return trainStation.getId();
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
     * 按主键删除经停站
     *
     * @param id 经停站ID
     */
    public void delete(Long id) {
        trainStationMapper.deleteById(id);
    }
}
