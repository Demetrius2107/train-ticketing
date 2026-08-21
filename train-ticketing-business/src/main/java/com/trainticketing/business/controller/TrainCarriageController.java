package com.trainticketing.business.controller;

import com.trainticketing.business.req.TrainCarriageSaveReq;
import com.trainticketing.business.resp.TrainCarriageQueryResp;
import com.trainticketing.business.service.TrainCarriageService;
import com.trainticketing.common.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: TrainCarriageController</p>
 * <p>Description: 车厢管理接口</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/admin/train-carriage")
public class TrainCarriageController {

    @Resource
    private TrainCarriageService trainCarriageService;

    /**
     * 新增车厢
     *
     * @param req 车厢新增请求（车次/厢号/座位类型/座位数）
     * @return 新增车厢ID
     */
    @PostMapping("/save")
    public CommonResp<Long> save(@Valid TrainCarriageSaveReq req) {
        return new CommonResp<>(trainCarriageService.save(req));
    }

    /**
     * 按车次查询车厢列表
     *
     * @param trainId 车次ID
     * @return 车厢列表（按厢号排序）
     */
    @GetMapping("/query")
    public CommonResp<List<TrainCarriageQueryResp>> query(@RequestParam Long trainId) {
        return new CommonResp<>(trainCarriageService.queryList(trainId));
    }

    /**
     * 删除车厢
     *
     * @param id 车厢ID
     * @return 空响应
     */
    @DeleteMapping("/delete")
    public CommonResp<Void> delete(@RequestParam Long id) {
        trainCarriageService.delete(id);
        return new CommonResp<>();
    }
}
