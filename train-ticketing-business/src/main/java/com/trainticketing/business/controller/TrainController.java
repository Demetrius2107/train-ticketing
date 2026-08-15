package com.trainticketing.business.controller;

import com.trainticketing.business.req.TrainSaveReq;
import com.trainticketing.business.resp.TrainQueryResp;
import com.trainticketing.business.service.TrainService;
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
 * <p>Title: TrainController</p>
 * <p>Description: 车次管理接口（走网关 http://localhost:8000/business/admin/train/**）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/admin/train")
public class TrainController {

    @Resource
    private TrainService trainService;

    /**
     * 新增车次
     *
     * @param req 车次新增请求（编号/类型/起终站/时刻）
     * @return 新增车次ID
     */
    @PostMapping("/save")
    public CommonResp<Long> save(@Valid TrainSaveReq req) {
        return new CommonResp<>(trainService.save(req));
    }

    /**
     * 查询车次列表
     *
     * @param keyword 查询关键字（车次编号模糊匹配），可空
     * @return 车次列表
     */
    @GetMapping("/query")
    public CommonResp<List<TrainQueryResp>> query(@RequestParam(required = false) String keyword) {
        return new CommonResp<>(trainService.queryList(keyword));
    }

    /**
     * 删除车次
     *
     * @param id 车次ID
     * @return 空响应
     */
    @DeleteMapping("/delete")
    public CommonResp<Void> delete(@RequestParam Long id) {
        trainService.delete(id);
        return new CommonResp<>();
    }
}
