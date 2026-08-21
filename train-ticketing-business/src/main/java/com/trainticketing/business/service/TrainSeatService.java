package com.trainticketing.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.business.domain.TrainCarriage;
import com.trainticketing.business.domain.TrainSeat;
import com.trainticketing.business.enums.SeatTypeEnum;
import com.trainticketing.business.mapper.TrainCarriageMapper;
import com.trainticketing.business.mapper.TrainSeatMapper;
import com.trainticketing.business.resp.TrainSeatQueryResp;
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
 * <p>Title: TrainSeatService</p>
 * <p>Description: 座位服务：按车厢批量生成座位档案（每排布局规则见 SeatTypeEnum）、按车次查询座位</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
@Service
public class TrainSeatService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainSeatService.class);

    @Resource
    private TrainSeatMapper trainSeatMapper;

    @Resource
    private TrainCarriageMapper trainCarriageMapper;

    /**
     * 按车厢批量生成座位档案。业务规则：
     * 1. 车厢必须已存在；
     * 2. 座位布局按座位类型决定（SeatTypeEnum 内置每排座位数与标签，如二等座每排5座 A B C D F）；
     * 3. 卧铺（硬卧/软卧）为铺位布局，阶段1 暂不支持生成，抛业务异常；
     * 4. 幂等保护：同一车厢只能生成一次，重复生成抛业务异常（uk_train_carriage_seat 兜底）；
     * 5. 排数 = ceil(座位数 / 每排座位数)，允许尾排不满（如61座二等座→13排65座）。
     *
     * @param carriageId 车厢ID
     * @return 生成的座位总数
     */
    public int generate(Long carriageId) {
        TrainCarriage carriage = trainCarriageMapper.selectById(carriageId);
        if (ObjectUtil.isNull(carriage)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_CARRIAGE_NOT_EXIST);
        }
        SeatTypeEnum seatTypeEnum = SeatTypeEnum.getByCode(carriage.getSeatType());
        if (ObjectUtil.isNull(seatTypeEnum) || CollUtil.isEmpty(seatTypeEnum.getSeatLabels())) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_SLEEPER_SEAT_NOT_SUPPORTED);
        }
        //幂等：车厢已生成过座位则拒绝
        if (trainSeatMapper.selectCountByCarriageId(carriageId) > 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_SEAT_ALREADY_GENERATED);
        }
        //排数 = ceil(座位数 / 每排座位数)
        int seatsPerRow = seatTypeEnum.getSeatsPerRow();
        int rowCount = (carriage.getSeatCount() + seatsPerRow - 1) / seatsPerRow;
        List<String> labels = seatTypeEnum.getSeatLabels();
        Date now = new Date();
        List<TrainSeat> seatList = new ArrayList<>();
        for (int row = 1; row <= rowCount; row++) {
            for (String label : labels) {
                TrainSeat seat = new TrainSeat();
                seat.setId(IdUtil.getSnowflakeNextId());
                seat.setTrainId(carriage.getTrainId());
                seat.setCarriageId(carriageId);
                seat.setSeatIndex(row);
                seat.setSeatLabel(label);
                seat.setSeatType(carriage.getSeatType());
                seat.setCreateTime(now);
                seat.setUpdateTime(now);
                seatList.add(seat);
            }
        }
        trainSeatMapper.insertBatch(seatList);
        return seatList.size();
    }

    /**
     * 按车次查询全部座位（座位图）
     *
     * @param trainId 车次ID
     * @return 座位列表（按车厢/排/字母排序）
     */
    public List<TrainSeatQueryResp> queryList(Long trainId) {
        List<TrainSeat> seatList = trainSeatMapper.selectByTrainId(trainId);
        List<TrainSeatQueryResp> respList = new ArrayList<>();
        if (CollUtil.isNotEmpty(seatList)) {
            for (TrainSeat seat : seatList) {
                respList.add(BeanUtil.copyProperties(seat, TrainSeatQueryResp.class));
            }
        }
        return respList;
    }
}
