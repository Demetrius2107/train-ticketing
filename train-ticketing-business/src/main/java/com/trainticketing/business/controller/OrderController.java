package com.trainticketing.business.controller;

import com.trainticketing.business.req.OrderSaveReq;
import com.trainticketing.business.resp.OrderQueryResp;
import com.trainticketing.business.service.OrderService;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import com.trainticketing.common.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: OrderController</p>
 * <p>Description: 订单接口 用户侧</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /** 网关注入的会员ID header（AuthFilter 写入），business 侧从此取当前登录会员 */
    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    /**
     * 从请求头取当前登录会员ID（由网关 AuthFilter 校验 token 后注入）。
     *
     * @param request HTTP 请求
     * @return 会员ID
     */
    private Long currentMemberId(HttpServletRequest request) {
        String id = request.getHeader(MEMBER_ID_HEADER);
        if (id == null || id.isBlank()) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_MEMBER_NOT_LOGIN);
        }
        return Long.valueOf(id);
    }

    /**
     * 下单：为每个乘车人分配区间可售座位，生成订单。
     * memberId 取自登录态（网关注入），不信任前端传入。
     *
     * @param req     下单请求
     * @param request HTTP 请求（取登录会员）
     * @return 订单号
     */
    @PostMapping("/save")
    public CommonResp<String> save(@RequestBody @Valid OrderSaveReq req, HttpServletRequest request) {
        req.setMemberId(currentMemberId(request));
        return new CommonResp<>(orderService.save(req));
    }

    /**
     * 异步下单（MQ 削峰）：请求只做校验、Lua 预扣与发消息，毫秒级返回排队订单号，
     * 选座出票由消费者异步完成。订单状态 4出票中 → 0待支付 / 5出票失败，
     * 前端凭返回的订单号轮询 /order/detail 获取终态。
     *
     * @param req     下单请求
     * @param request HTTP 请求（取登录会员）
     * @return 排队订单号
     */
    @PostMapping("/async")
    public CommonResp<String> saveAsync(@RequestBody @Valid OrderSaveReq req, HttpServletRequest request) {
        req.setMemberId(currentMemberId(request));
        return new CommonResp<>(orderService.saveAsync(req));
    }

    /**
     * 查询当前登录会员的订单列表（含明细）
     *
     * @param request HTTP 请求（取登录会员）
     * @return 订单列表
     */
    @GetMapping("/list")
    public CommonResp<List<OrderQueryResp>> list(HttpServletRequest request) {
        return new CommonResp<>(orderService.queryByMemberId(currentMemberId(request)));
    }

    /**
     * 查询订单详情（含明细）
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    @GetMapping("/detail")
    public CommonResp<OrderQueryResp> detail(@org.springframework.web.bind.annotation.RequestParam String orderNo) {
        return new CommonResp<>(orderService.queryByOrderNo(orderNo));
    }

    /**
     * 取消订单（仅待支付订单；释放区间占用，余票恢复）
     *
     * @param orderNo 订单号
     * @return 成功
     */
    @PostMapping("/cancel")
    public CommonResp<Void> cancel(@org.springframework.web.bind.annotation.RequestParam String orderNo) {
        orderService.cancel(orderNo);
        return new CommonResp<>();
    }

    /**
     * 订单支付（仅待支付且未过期订单；成功后状态置已支付并记录支付时间）。
     * memberId 取自登录态，确保只有订单归属会员可支付。
     *
     * @param orderNo 订单号
     * @param request HTTP 请求（取登录会员）
     * @return 成功
     */
    @PostMapping("/pay")
    public CommonResp<Void> pay(@org.springframework.web.bind.annotation.RequestParam String orderNo,
                                HttpServletRequest request) {
        orderService.pay(orderNo, currentMemberId(request));
        return new CommonResp<>();
    }

    /**
     * 退票（仅已支付订单；释放区间占用，余票恢复，状态置已退票）。
     * memberId 取自登录态，确保只有订单归属会员可退。
     *
     * @param orderNo 订单号
     * @param request HTTP 请求（取登录会员）
     * @return 成功
     */
    @PostMapping("/refund")
    public CommonResp<Void> refund(@org.springframework.web.bind.annotation.RequestParam String orderNo,
                                   HttpServletRequest request) {
        orderService.refund(orderNo, currentMemberId(request));
        return new CommonResp<>();
    }
}
