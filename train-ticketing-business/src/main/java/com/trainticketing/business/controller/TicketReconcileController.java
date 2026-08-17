package com.trainticketing.business.controller;

import com.trainticketing.business.service.TicketReconcileService;
import com.trainticketing.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: TicketReconcileController</p>
 * <p>Description: 余票缓存对账接口（运维用，走网关 http://localhost:8000/business/reconcile/**）。
 * 定时任务每小时自动执行，此处提供手动触发入口用于排障/验证。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-17
 * @updateTime 2026-08-17
 */
@RestController
@RequestMapping("/reconcile")
public class TicketReconcileController {

    @Resource
    private TicketReconcileService ticketReconcileService;

    /**
     * 手动触发全量对账（重建所有运行中排班的余票缓存）
     *
     * @return 成功
     */
    @PostMapping("/all")
    public CommonResp<Void> reconcileAll() {
        ticketReconcileService.reconcileAll();
        return new CommonResp<>();
    }

    /**
     * 手动触发单排班对账
     *
     * @param dailyTrainId 排班ID
     * @return 重建的座位类型数
     */
    @GetMapping("/{dailyTrainId}")
    public CommonResp<Integer> reconcileOne(@PathVariable Long dailyTrainId) {
        return new CommonResp<>(ticketReconcileService.reconcileDailyTrain(dailyTrainId));
    }
}
