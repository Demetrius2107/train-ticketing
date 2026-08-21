package com.trainticketing.business.controller;

import com.trainticketing.business.req.StationSaveReq;
import com.trainticketing.business.resp.StationQueryResp;
import com.trainticketing.business.service.StationService;
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
 * <p>Title: StationController</p>
 * <p>Description: 车站管理接口</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/admin/station")
public class StationController {

    @Resource
    private StationService stationService;

    /**
     * 新增车站
     *
     * @param req 车站新增请求（名称/拼音/城市）
     * @return 新增车站ID
     */
    @PostMapping("/save")
    public CommonResp<Long> save(@Valid StationSaveReq req) {
        return new CommonResp<>(stationService.save(req));
    }

    /**
     * 查询车站列表
     *
     * @param keyword 查询关键字（名称/拼音/城市模糊匹配），可空
     * @return 车站列表
     */
    @GetMapping("/query")
    public CommonResp<List<StationQueryResp>> query(@RequestParam(required = false) String keyword) {
        return new CommonResp<>(stationService.queryList(keyword));
    }

    /**
     * 删除车站
     *
     * @param id 车站ID
     * @return 空响应
     */
    @DeleteMapping("/delete")
    public CommonResp<Void> delete(@RequestParam Long id) {
        stationService.delete(id);
        return new CommonResp<>();
    }
}
