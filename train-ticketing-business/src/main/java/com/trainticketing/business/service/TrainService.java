package com.trainticketing.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.Station;
import com.trainticketing.business.domain.Train;
import com.trainticketing.business.mapper.StationMapper;
import com.trainticketing.business.mapper.TrainMapper;
import com.trainticketing.business.req.TrainSaveReq;
import com.trainticketing.business.resp.TrainQueryResp;
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
 * <p>Title: TrainService</p>
 * <p>Description: 车次管理服务：新增（编号唯一/车站引用校验）、关键字查询、删除</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
@Service
public class TrainService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainService.class);

    @Resource
    private TrainMapper trainMapper;

    @Resource
    private StationMapper stationMapper;

    /**
     * 新增车次。业务规则：
     * 1. 车次编号全局唯一（代码校验 + uk_code 唯一索引兜底）；
     * 2. 始发站/终到站必须已存在（引用校验）；
     * 3. 始发站与终到站不能相同；
     * 4. 车次允许跨天运行（如凌晨到达），故不强制 endTime &gt; startTime。
     *
     * @param req 车次新增请求
     * @return 新增车次ID
     */
    public Long save(TrainSaveReq req) {
        Train trainDB = trainMapper.selectByCode(req.getCode());
        if (ObjectUtil.isNotNull(trainDB)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_CODE_UNIQUE_ERROR);
        }
        Station startStation = stationMapper.selectById(req.getStartStationId());
        if (ObjectUtil.isNull(startStation)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_STATION_NOT_EXIST);
        }
        Station endStation = stationMapper.selectById(req.getEndStationId());
        if (ObjectUtil.isNull(endStation)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_STATION_NOT_EXIST);
        }
        if (req.getStartStationId().equals(req.getEndStationId())) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_START_END_STATION_SAME);
        }
        Train train = new Train();
        train.setId(IdUtil.getSnowflakeNextId());
        train.setCode(req.getCode());
        train.setType(req.getType());
        train.setStartStationId(req.getStartStationId());
        train.setEndStationId(req.getEndStationId());
        train.setStartTime(LocalTime.parse(req.getStartTime()));
        train.setEndTime(LocalTime.parse(req.getEndTime()));
        train.setCreateTime(new Date());
        train.setUpdateTime(new Date());
        trainMapper.insert(train);
        return train.getId();
    }

    /**
     * 按关键字查询车次列表（车次编号模糊匹配）
     *
     * @param keyword 查询关键字，可空（空则返回全部）
     * @return 车次列表
     */
    public List<TrainQueryResp> queryList(String keyword) {
        List<Train> trainList = trainMapper.selectList(keyword);
        List<TrainQueryResp> respList = new ArrayList<>();
        if (CollUtil.isNotEmpty(trainList)) {
            for (Train train : trainList) {
                respList.add(BeanUtil.copyProperties(train, TrainQueryResp.class));
            }
        }
        return respList;
    }

    /**
     * 按主键删除车次
     * 注意：当前为物理删除，若后续经停站/车厢/每日排班引用该车次，需补充引用保护。
     *
     * @param id 车次ID
     */
    public void delete(Long id) {
        trainMapper.deleteById(id);
    }
}
