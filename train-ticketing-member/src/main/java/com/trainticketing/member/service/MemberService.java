package com.trainticketing.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import com.trainticketing.common.util.JwtUtil;
import com.trainticketing.member.domain.Member;
import com.trainticketing.member.domain.MemberExample;
import com.trainticketing.member.domain.SmsCode;
import com.trainticketing.member.mapper.MemberMapper;
import com.trainticketing.member.mapper.SmsCodeMapper;
import com.trainticketing.member.resp.MemberLoginResp;
import com.trainticketing.req.MemberLoginReq;
import com.trainticketing.req.MemberRegisterReq;
import com.trainticketing.req.MemberSendCodeReq;
import jakarta.annotation.Resource;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private static final Logger LOG = LoggerFactory.getLogger(MemberService.class);


    @Resource
    private MemberMapper memberMapper;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @Resource
    private JwtUtil jwtUtil;

    public int count() {
        return Math.toIntExact(memberMapper.countByExample(null));
    }

    public long register(MemberRegisterReq req) {
        String mobile = req.getMobile();
        Member memberDB = selectByMobile(mobile);
        if (ObjectUtil.isNotNull(memberDB)) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_EXIST);
        }
        Member member = new Member();
        member.setId(IdUtil.getSnowflakeNextId());
        member.setMobile(mobile);
        memberMapper.insert(member);
        return member.getId();
    }

    public void sendCode(MemberSendCodeReq req) {
        String mobile = req.getMobile();
        Member memberDB = selectByMobile(mobile);
        //如果手机号不存在，则插入一条记录
        if (ObjectUtil.isNull(memberDB)) {
            Member member = new Member();
            member.setId(IdUtil.getSnowflakeNextId());
            member.setMobile(mobile);
            memberMapper.insert(member);
        }
        //生成验证码（6位数字）
        String code = RandomUtil.randomNumbers(6);
        //保存短信记录表 手机号 短信验证码 有效期 是否已经使用 业务类型 发送时间 使用时间
        SmsCode smsCode = new SmsCode();
        smsCode.setId(IdUtil.getSnowflakeNextId());
        smsCode.setMobile(mobile);
        smsCode.setCode(code);
        smsCode.setType("LOGIN");
        smsCode.setExpiredAt(DateUtil.offsetMinute(new Date(), 5));
        smsCode.setUsedFlag("0");
        smsCode.setCreateTime(new Date());
        smsCodeMapper.insert(smsCode);
        LOG.info("生成短信验证码:{}", code);
        //对接短信通道，发送短信（开发环境占位）
        LOG.info("对接短信通道");
    }


    public MemberLoginResp login(MemberLoginReq req) {
        String mobile = req.getMobile();
        String code = req.getCode();
        Member memberDB = selectByMobile(mobile);
        //如果手机号不存在，则插入一条记录
        if (ObjectUtil.isNull(memberDB)) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_NOT_EXIST);
        }

        //校验短信验证码：取该手机号最新一条未使用的验证码
        SmsCode smsCode = smsCodeMapper.selectLatestUnusedByMobile(mobile, "LOGIN");
        //验证码不存在或不匹配
        if (ObjectUtil.isNull(smsCode) || !code.equals(smsCode.getCode())) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_CODE_ERROR);
        }
        //验证码已过期
        if (smsCode.getExpiredAt().before(new Date())) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_CODE_EXPIRED);
        }
        //验证码校验通过，标记已使用
        smsCodeMapper.markUsed(smsCode.getId(), new Date());
        MemberLoginResp resp = BeanUtil.copyProperties(memberDB, MemberLoginResp.class);
        // 签发 JWT，前端后续请求带 Authorization: Bearer {token}
        resp.setToken(jwtUtil.generate(memberDB.getId(), memberDB.getMobile()));
        return resp;

    }

    private Member selectByMobile(String mobile) {
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> list = memberMapper.selectByExample(memberExample);
        if (CollUtil.isNotEmpty(list)) {
            return list.get(0);
        } else {
            return null;
        }
    }

}