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

  /**
   * 重复购票校验：统计指定车次/乘车日期下，给定身份证集合中已持有
   * 与 [departIndex, arriveIndex] 区间重叠车票的数量。
   * 明细行只在订单存续期存在（取消/退票/关单即删除），故无需再过滤订单状态。
   *
   * @param trainId     车次ID
   * @param runDate     乘车日期
   * @param idCards     身份证号列表
   * @param departIndex 出发站序
   * @param arriveIndex 到达站序
   * @return 命中的既有车票数
   */
  int countOverlapByIdCards(@Param("trainId") Long trainId,
                            @Param("runDate") java.util.Date runDate,
                            @Param("idCards") List<String> idCards,
                            @Param("departIndex") Integer departIndex,
                            @Param("arriveIndex") Integer arriveIndex);
}
