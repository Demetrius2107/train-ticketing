package com.trainticketing.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.Train;
import com.trainticketing.business.domain.TrainCarriage;
import com.trainticketing.business.mapper.TrainCarriageMapper;
import com.trainticketing.business.mapper.TrainMapper;
import com.trainticketing.business.req.TrainCarriageSaveReq;
import com.trainticketing.business.resp.TrainCarriageQueryResp;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <p>Title: TrainCarriageService</p>
 * <p>Description: 车厢管理服务：新增（车次引用/厢号唯一校验）、按车次查询、删除</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
@Service
public class TrainCarriageService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainCarriageService.class);

    @Resource
    private TrainCarriageMapper trainCarriageMapper;

    @Resource
    private TrainMapper trainMapper;

    /**
     * 新增车厢。业务规则：
     * 1. 车次必须已存在（引用校验）；
     * 2. 同一车次厢号唯一（代码校验 + uk_train_carriage_index 唯一索引兜底）；
     * 3. 座位类型与座位数在 Req 校验（类型 1-5、数量 ≥ 1）。
     *
     * @param req 车厢新增请求
     * @return 新增车厢ID
     */
    public Long save(TrainCarriageSaveReq req) {
        Train train = trainMapper.selectById(req.getTrainId());
        if (ObjectUtil.isNull(train)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_NOT_EXIST);
        }
        TrainCarriage carriageDB = trainCarriageMapper.selectByIndex(req.getTrainId(), req.getCarriageIndex());
        if (ObjectUtil.isNotNull(carriageDB)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_CARRIAGE_INDEX_UNIQUE_ERROR);
        }
        TrainCarriage carriage = new TrainCarriage();
        carriage.setId(IdUtil.getSnowflakeNextId());
        carriage.setTrainId(req.getTrainId());
        carriage.setCarriageIndex(req.getCarriageIndex());
        carriage.setSeatType(req.getSeatType());
        carriage.setSeatCount(req.getSeatCount());
        carriage.setCreateTime(new Date());
        carriage.setUpdateTime(new Date());
        trainCarriageMapper.insert(carriage);
        return carriage.getId();
    }

    /**
     * 按车次查询车厢列表（按厢号排序）
     *
     * @param trainId 车次ID
     * @return 车厢列表
     */
    public List<TrainCarriageQueryResp> queryList(Long trainId) {
        List<TrainCarriage> carriageList = trainCarriageMapper.selectByTrainId(trainId);
        List<TrainCarriageQueryResp> respList = new ArrayList<>();
        if (CollUtil.isNotEmpty(carriageList)) {
            for (TrainCarriage carriage : carriageList) {
                respList.add(BeanUtil.copyProperties(carriage, TrainCarriageQueryResp.class));
            }
        }
        return respList;
    }

    /**
     * 按主键删除车厢
     * 注意：若该车厢已生成座位档案，删除需补充引用保护（阶段1 暂以注释声明）。
     *
     * @param id 车厢ID
     */
    public void delete(Long id) {
        trainCarriageMapper.deleteById(id);
    }
}
