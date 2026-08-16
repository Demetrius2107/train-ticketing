package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.TrainOrderItem;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: TrainOrderItemMapper</p>
 * <p>Description: 订单明细表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface TrainOrderItemMapper {

  /**
   * 新增订单明细
   *
   * @param record 订单明细实体
   * @return 影响行数
   */
  int insert(TrainOrderItem record);

  /**
   * 按订单查询明细列表
   *
   * @param orderId 订单ID
   * @return 明细列表
   */
  List<TrainOrderItem> selectByOrderId(@Param("orderId") Long orderId);

  /**
   * 按订单删除明细（订单取消/退票时释放占用）
   *
   * @param orderId 订单ID
   * @return 影响行数
   */
  int deleteByOrderId(@Param("orderId") Long orderId);
}
