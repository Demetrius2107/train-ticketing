package com.trainticketing.member.controller;

import com.trainticketing.common.resp.CommonResp;
import com.trainticketing.member.resp.MemberLoginResp;
import com.trainticketing.member.service.MemberService;
import com.trainticketing.req.MemberLoginReq;
import com.trainticketing.req.MemberRegisterReq;
import com.trainticketing.req.MemberSendCodeReq;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Resource
    private MemberService memberService;

    @GetMapping("/count")
    public CommonResp<Integer> count() {
        int count = memberService.count();
        CommonResp<Integer> commonResp = new CommonResp<>();
        commonResp.setContent(count);
        return commonResp;
    }

    @PostMapping("/register")
    public CommonResp<Long> register(@Valid MemberRegisterReq req) {
        long register = memberService.register(req);
        return new CommonResp<>(register);
    }


    @PostMapping("/send-code")
    public CommonResp<String> sendCode(@Valid MemberSendCodeReq req) {
        // content 为验证码（占位通道开发模式直返，见 member.sms.mock-return-code）；生产为 null
        return new CommonResp<>(memberService.sendCode(req));
    }

    @PostMapping("/login")
    public CommonResp<MemberLoginResp> login(@Valid MemberLoginReq req) {
        MemberLoginResp resp = memberService.login(req);
        return new CommonResp<>(resp);
    }

}
