package com.trainticketing.business.controller;

import com.trainticketing.business.req.TrainPriceSaveReq;
import com.trainticketing.business.resp.TrainPriceQueryResp;
import com.trainticketing.business.service.TrainPriceService;
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
 * <p>Title: TrainPriceController</p>
 * <p>Description: 票价管理接口（走网关 http://localhost:8000/business/admin/train-price/**）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/admin/train-price")
public class TrainPriceController {

    @Resource
    private TrainPriceService trainPriceService;

    /**
     * 新增票价
     *
     * @param req 票价新增请求（车次/座位类型/票价）
     * @return 新增票价ID
     */
    @PostMapping("/save")
    public CommonResp<Long> save(@Valid TrainPriceSaveReq req) {
        return new CommonResp<>(trainPriceService.save(req));
    }

    /**
     * 按车次查询票价列表
     *
     * @param trainId 车次ID
     * @return 票价列表（按座位类型排序）
     */
    @GetMapping("/query")
    public CommonResp<List<TrainPriceQueryResp>> query(@RequestParam Long trainId) {
        return new CommonResp<>(trainPriceService.queryList(trainId));
    }

    /**
     * 删除票价
     *
     * @param id 票价ID
     * @return 空响应
     */
    @DeleteMapping("/delete")
    public CommonResp<Void> delete(@RequestParam Long id) {
        trainPriceService.delete(id);
        return new CommonResp<>();
    }
}
