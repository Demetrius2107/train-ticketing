package com.trainticketing.member.controller;

import com.trainticketing.common.resp.CommonResp;
import com.trainticketing.member.domain.Passenger;
import com.trainticketing.member.service.PassengerService;
import com.trainticketing.req.PassengerSaveReq;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: PassengerController</p>
 * <p>Description: 乘车人接口（用户侧，走网关 http://localhost:8000/member/passenger/**）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
@RestController
@RequestMapping("/passenger")
public class PassengerController {

    @Resource
    private PassengerService passengerService;

    /**
     * 新增乘车人
     *
     * @param req 乘车人请求
     * @return 乘车人ID
     */
    @PostMapping("/save")
    public CommonResp<Long> save(@RequestBody @Valid PassengerSaveReq req) {
        return new CommonResp<>(passengerService.save(req));
    }

    /**
     * 查询会员乘车人列表
     *
     * @param memberId 会员id
     * @return 乘车人列表
     */
    @GetMapping("/list")
    public CommonResp<List<Passenger>> list(@RequestParam Long memberId) {
        return new CommonResp<>(passengerService.list(memberId));
    }

    /**
     * 删除乘车人
     *
     * @param id 乘车人id
     * @return 成功
     */
    @DeleteMapping("/delete")
    public CommonResp<Void> delete(@RequestParam Long id) {
        passengerService.delete(id);
        return new CommonResp<>();
    }
}
