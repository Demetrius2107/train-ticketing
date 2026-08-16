package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.TrainOrder;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: TrainOrderMapper</p>
 * <p>Description: 订单表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface TrainOrderMapper {

  /**
   * 新增订单
   *
   * @param record 订单实体
   * @return 影响行数
   */
  int insert(TrainOrder record);

  /**
   * 按主键查询订单
   *
   * @param id 订单ID
   * @return 订单，不存在返回 null
   */
  TrainOrder selectById(@Param("id") Long id);

  /**
   * 按订单号查询订单
   *
   * @param orderNo 订单号
   * @return 订单，不存在返回 null
   */
  TrainOrder selectByOrderNo(@Param("orderNo") String orderNo);

  /**
   * 按会员查询订单列表（按下单时间倒序）
   *
   * @param memberId 会员ID
   * @return 订单列表
   */
  List<TrainOrder> selectByMemberId(@Param("memberId") Long memberId);

  /**
   * 按主键更新订单（状态/支付时间等）
   *
   * @param record 订单实体（非空字段更新）
   * @return 影响行数
   */
  int updateById(TrainOrder record);
}
