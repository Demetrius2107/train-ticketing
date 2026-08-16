package com.trainticketing.business.controller;

import com.trainticketing.business.req.OrderSaveReq;
import com.trainticketing.business.service.OrderService;
import com.trainticketing.common.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: OrderController</p>
 * <p>Description: 订单接口（用户侧，走网关 http://localhost:8000/business/order/**）</p>
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

    /**
     * 下单：为每个乘车人分配区间可售座位，生成订单
     *
     * @param req 下单请求
     * @return 订单号
     */
    @PostMapping("/save")
    public CommonResp<String> save(@RequestBody @Valid OrderSaveReq req) {
        return new CommonResp<>(orderService.save(req));
    }
}
