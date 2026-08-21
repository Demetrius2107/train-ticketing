package com.trainticketing.business.controller;

import com.trainticketing.business.resp.TrainSeatQueryResp;
import com.trainticketing.business.service.TrainSeatService;
import com.trainticketing.common.resp.CommonResp;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: TrainSeatController</p>
 * <p>Description: 座位接口</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/admin/train-seat")
public class TrainSeatController {

    @Resource
    private TrainSeatService trainSeatService;

    /**
     * 按车厢批量生成座位档案
     *
     * @param carriageId 车厢ID
     * @return 生成的座位总数
     */
    @PostMapping("/generate")
    public CommonResp<Integer> generate(@RequestParam Long carriageId) {
        return new CommonResp<>(trainSeatService.generate(carriageId));
    }

    /**
     * 按车次查询座位列表（座位图）
     *
     * @param trainId 车次ID
     * @return 座位列表
     */
    @GetMapping("/query")
    public CommonResp<List<TrainSeatQueryResp>> query(@RequestParam Long trainId) {
        return new CommonResp<>(trainSeatService.queryList(trainId));
    }
}
