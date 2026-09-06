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

  /**
   * CAS 式状态转换（乐观锁防并发竞态）：仅当订单当前状态等于 expectStatus 时，
   * 才更新为新状态并可选记录支付/退款时间。返回影响行数，0 表示状态已被并发改动需放弃操作。
   * <p>解决场景：用户在支付过期边缘点支付，同时关单任务在跑，两者只有一个能成功。
   *
   * @param id           订单ID
   * @param expectStatus 期望的当前状态（条件）
   * @param newStatus    目标状态
   * @param payTime      支付时间（可空，仅支付时传）
   * @param refundTime   退款时间（可空，仅退票时传）
   * @return 影响行数（1 成功 / 0 状态不匹配）
   */
  int updateStatusIfMatch(@Param("id") Long id,
                          @Param("expectStatus") String expectStatus,
                          @Param("newStatus") String newStatus,
                          @Param("payTime") java.util.Date payTime,
                          @Param("refundTime") java.util.Date refundTime);

  /**
   * 查询已超过支付过期时间且仍为待支付的订单（延时关单扫描用）
   *
   * @param now 当前时间
   * @return 超时待支付订单列表
   */
  List<TrainOrder> selectExpiredPending(@Param("now") java.util.Date now);

  /**
   * 查询出票中且已超过出票窗口的悬挂订单（兜底扫描用）：
   * 出票消息丢失或重试耗尽进死信后，订单停留出票中状态，由扫描收敛为出票失败
   *
   * @param threshold 出票截止时间（expire_time ≤ threshold 视为悬挂）
   * @return 悬挂出票中订单列表
   */
  List<TrainOrder> selectHungQueuing(@Param("threshold") java.util.Date threshold);
}
