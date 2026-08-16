package com.trainticketing.member.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import com.trainticketing.member.domain.Passenger;
import com.trainticketing.member.enums.PassengerTypeEnum;
import com.trainticketing.member.mapper.PassengerMapper;
import com.trainticketing.req.PassengerSaveReq;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <p>Title: PassengerService</p>
 * <p>Description: 乘车人服务：新增/列表/删除（下单时乘车人快照来源）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@Service
public class PassengerService {

    private static final Logger LOG = LoggerFactory.getLogger(PassengerService.class);

    @Resource
    private PassengerMapper passengerMapper;

    /**
     * 新增乘车人（校验旅客类型合法性）
     *
     * @param req 乘车人请求
     * @return 乘车人ID
     */
    public Long save(PassengerSaveReq req) {
        if (ObjectUtil.isNull(PassengerTypeEnum.getByCode(req.getType()))) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_PASSENGER_TYPE_INVALID);
        }
        long now = System.currentTimeMillis();
        Passenger passenger = new Passenger();
        passenger.setId(IdUtil.getSnowflakeNextId());
        passenger.setMemberId(req.getMemberId());
        passenger.setName(req.getName());
        passenger.setIdCard(req.getIdCard());
        passenger.setType(req.getType());
        passenger.setCreateTime(new Date(now));
        passenger.setUpdateTime(new Date(now));
        passengerMapper.insert(passenger);
        LOG.info("新增乘车人 id={}, memberId={}, name={}", passenger.getId(), req.getMemberId(), req.getName());
        return passenger.getId();
    }

    /**
     * 查询会员乘车人列表
     *
     * @param memberId 会员ID
     * @return 乘车人列表
     */
    public List<Passenger> list(Long memberId) {
        return passengerMapper.selectByMemberId(memberId);
    }

    /**
     * 删除乘车人
     *
     * @param id 乘车人ID
     */
    public void delete(Long id) {
        Passenger passenger = passengerMapper.selectById(id);
        if (ObjectUtil.isNull(passenger)) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_PASSENGER_NOT_EXIST);
        }
        passengerMapper.deleteById(id);
        LOG.info("删除乘车人 id={}", id);
    }
}
