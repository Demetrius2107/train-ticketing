package com.trainticketing.business.controller;

import com.trainticketing.business.req.DailyTrainSaveReq;
import com.trainticketing.business.resp.DailyTrainQueryResp;
import com.trainticketing.business.service.DailyTrainService;
import com.trainticketing.common.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: DailyTrainController</p>
 * <p>Description: 每日排班管理接口（走网关 http://localhost:8000/business/admin/daily-train/**）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/admin/daily-train")
public class DailyTrainController {

    @Resource
    private DailyTrainService dailyTrainService;

    /**
     * 新增排班
     *
     * @param req 排班新增请求（车次/运行日期）
     * @return 新增排班ID
     */
    @PostMapping("/save")
    public CommonResp<Long> save(@Valid DailyTrainSaveReq req) {
        return new CommonResp<>(dailyTrainService.save(req));
    }

    /**
     * 查询排班列表
     *
     * @param trainId 车次ID，可空
     * @param runDate 运行日期，可空
     * @return 排班列表
     */
    @GetMapping("/query")
    public CommonResp<List<DailyTrainQueryResp>> query(@RequestParam(required = false) Long trainId,
                                                       @RequestParam(required = false) LocalDate runDate) {
        return new CommonResp<>(dailyTrainService.queryList(trainId, runDate));
    }

    /**
     * 更新排班状态（0 停运 / 1 运行）
     *
     * @param id     排班ID
     * @param status 目标状态
     * @return 空响应
     */
    @PostMapping("/update-status")
    public CommonResp<Void> updateStatus(@RequestParam Long id, @RequestParam String status) {
        dailyTrainService.updateStatus(id, status);
        return new CommonResp<>();
    }

    /**
     * 删除排班
     *
     * @param id 排班ID
     * @return 空响应
     */
    @DeleteMapping("/delete")
    public CommonResp<Void> delete(@RequestParam Long id) {
        dailyTrainService.delete(id);
        return new CommonResp<>();
    }
}
