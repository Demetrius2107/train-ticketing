package com.trainticketing.business.controller;

import com.trainticketing.business.req.TrainStationSaveReq;
import com.trainticketing.business.resp.TrainStationQueryResp;
import com.trainticketing.business.service.TrainStationService;
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
 * <p>Title: TrainStationController</p>
 * <p>Description: 车次经停站管理接口（走网关 http://localhost:8000/business/admin/train-station/**）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/admin/train-station")
public class TrainStationController {

    @Resource
    private TrainStationService trainStationService;

    /**
     * 新增经停站
     *
     * @param req 经停站新增请求（车次/车站/站序/时刻）
     * @return 新增经停站ID
     */
    @PostMapping("/save")
    public CommonResp<Long> save(@Valid TrainStationSaveReq req) {
        return new CommonResp<>(trainStationService.save(req));
    }

    /**
     * 按车次查询经停站列表
     *
     * @param trainId 车次ID
     * @return 经停站列表（按站序排序）
     */
    @GetMapping("/query")
    public CommonResp<List<TrainStationQueryResp>> query(@RequestParam Long trainId) {
        return new CommonResp<>(trainStationService.queryList(trainId));
    }

    /**
     * 删除经停站
     *
     * @param id 经停站ID
     * @return 空响应
     */
    @DeleteMapping("/delete")
    public CommonResp<Void> delete(@RequestParam Long id) {
        trainStationService.delete(id);
        return new CommonResp<>();
    }
}
