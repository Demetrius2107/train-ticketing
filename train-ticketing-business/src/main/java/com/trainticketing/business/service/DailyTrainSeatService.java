package com.trainticketing.business.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.DailyTrain;
import com.trainticketing.business.domain.DailyTrainSeat;
import com.trainticketing.business.domain.TrainSeat;
import com.trainticketing.business.mapper.DailyTrainMapper;
import com.trainticketing.business.mapper.DailyTrainSeatMapper;
import com.trainticketing.business.mapper.TrainSeatMapper;
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
 * <p>Title: DailyTrainSeatService</p>
 * <p>Description: 当日座位服务：按排班批量生成当日座位（售卖状态初始为可售），供余票查询/购票使用</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@Service
public class DailyTrainSeatService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainSeatService.class);

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    @Resource
    private DailyTrainMapper dailyTrainMapper;

    @Resource
    private TrainSeatMapper trainSeatMapper;

    /**
     * 按排班批量生成当日座位。业务规则：
     * 1. 排班必须已存在（引用校验）；
     * 2. 幂等：同一排班只能生成一次，重复生成抛业务异常（uk_daily_train_seat 兜底）；
     * 3. 数据来源：从该车次的座位档案（train_seat）复制，saleStatus 初始为 0 可售；
     * 4. 生成后该排班才具备余票查询与购票能力（阶段1 手动触发，后续可改为排班时自动生成）。
     *
     * @param dailyTrainId 排班ID
     * @return 生成的当日座位总数
     */
    public int generate(Long dailyTrainId) {
        DailyTrain dailyTrain = dailyTrainMapper.selectById(dailyTrainId);
        if (ObjectUtil.isNull(dailyTrain)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_DAILY_TRAIN_NOT_EXIST);
        }
        //幂等：排班已生成过当日座位则拒绝
        if (dailyTrainSeatMapper.selectCountByDailyTrainId(dailyTrainId) > 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_DAILY_SEAT_ALREADY_GENERATED);
        }
        List<TrainSeat> trainSeatList = trainSeatMapper.selectByTrainId(dailyTrain.getTrainId());
        if (CollUtil.isEmpty(trainSeatList)) {
            //车次尚未生成座位档案，提示先生成座位
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_SEAT_NOT_GENERATED);
        }
        Date now = new Date();
        List<DailyTrainSeat> seatList = new ArrayList<>();
        for (TrainSeat trainSeat : trainSeatList) {
            DailyTrainSeat dailyTrainSeat = new DailyTrainSeat();
            dailyTrainSeat.setId(IdUtil.getSnowflakeNextId());
            dailyTrainSeat.setDailyTrainId(dailyTrainId);
            dailyTrainSeat.setTrainSeatId(trainSeat.getId());
            dailyTrainSeat.setCarriageId(trainSeat.getCarriageId());
            dailyTrainSeat.setSeatIndex(trainSeat.getSeatIndex());
            dailyTrainSeat.setSeatLabel(trainSeat.getSeatLabel());
            dailyTrainSeat.setSeatType(trainSeat.getSeatType());
            dailyTrainSeat.setSaleStatus("0");
            dailyTrainSeat.setCreateTime(now);
            dailyTrainSeat.setUpdateTime(now);
            seatList.add(dailyTrainSeat);
        }
        dailyTrainSeatMapper.insertBatch(seatList);
        return seatList.size();
    }
}
