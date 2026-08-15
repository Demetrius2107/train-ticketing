package com.trainticketing.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.Train;
import com.trainticketing.business.domain.TrainPrice;
import com.trainticketing.business.mapper.TrainMapper;
import com.trainticketing.business.mapper.TrainPriceMapper;
import com.trainticketing.business.req.TrainPriceSaveReq;
import com.trainticketing.business.resp.TrainPriceQueryResp;
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
 * <p>Title: TrainPriceService</p>
 * <p>Description: 票价管理服务：新增（车次引用/车次+座位类型唯一）、按车次查询、删除</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@Service
public class TrainPriceService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainPriceService.class);

    @Resource
    private TrainPriceMapper trainPriceMapper;

    @Resource
    private TrainMapper trainMapper;

    /**
     * 新增票价。业务规则：
     * 1. 车次必须已存在（引用校验）；
     * 2. 同一车次同一座位类型票价唯一（代码校验 + uk_train_seat_type 唯一索引兜底）；
     * 3. 票价使用 BigDecimal，精度 decimal(10,2)，Req 校验 &gt; 0。
     *
     * @param req 票价新增请求
     * @return 新增票价ID
     */
    public Long save(TrainPriceSaveReq req) {
        Train train = trainMapper.selectById(req.getTrainId());
        if (ObjectUtil.isNull(train)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_NOT_EXIST);
        }
        TrainPrice priceDB = trainPriceMapper.selectByTrainAndType(req.getTrainId(), req.getSeatType());
        if (ObjectUtil.isNotNull(priceDB)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_PRICE_EXIST);
        }
        TrainPrice trainPrice = new TrainPrice();
        trainPrice.setId(IdUtil.getSnowflakeNextId());
        trainPrice.setTrainId(req.getTrainId());
        trainPrice.setSeatType(req.getSeatType());
        trainPrice.setPrice(req.getPrice());
        trainPrice.setCreateTime(new Date());
        trainPrice.setUpdateTime(new Date());
        trainPriceMapper.insert(trainPrice);
        return trainPrice.getId();
    }

    /**
     * 按车次查询票价列表（按座位类型排序）
     *
     * @param trainId 车次ID
     * @return 票价列表
     */
    public List<TrainPriceQueryResp> queryList(Long trainId) {
        List<TrainPrice> priceList = trainPriceMapper.selectByTrainId(trainId);
        List<TrainPriceQueryResp> respList = new ArrayList<>();
        if (CollUtil.isNotEmpty(priceList)) {
            for (TrainPrice trainPrice : priceList) {
                respList.add(BeanUtil.copyProperties(trainPrice, TrainPriceQueryResp.class));
            }
        }
        return respList;
    }

    /**
     * 按主键删除票价
     *
     * @param id 票价ID
     */
    public void delete(Long id) {
        trainPriceMapper.deleteById(id);
    }
}
