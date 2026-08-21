package com.trainticketing.business.controller;

import com.trainticketing.business.service.DailyTrainSeatService;
import com.trainticketing.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: DailyTrainSeatController</p>
 * <p>Description: 当日座位接口 走网关 </p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/admin/daily-train-seat")
public class DailyTrainSeatController {

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    /**
     * 按排班批量生成当日座位
     *
     * @param dailyTrainId 排班ID
     * @return 生成的当日座位总数
     */
    @PostMapping("/generate")
    public CommonResp<Integer> generate(@RequestParam Long dailyTrainId) {
        return new CommonResp<>(dailyTrainSeatService.generate(dailyTrainId));
    }
}
