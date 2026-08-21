package com.trainticketing.member.mapper;

import com.trainticketing.member.domain.SmsCode;

import java.util.Date;

import org.apache.ibatis.annotations.Param;

public interface SmsCodeMapper {

    int insert(SmsCode record);

    /**
     * 查询指定手机号、业务类型下最新一条未使用的验证码
     */
    SmsCode selectLatestUnusedByMobile(@Param("mobile") String mobile, @Param("type") String type);

    /**
     * 标记验证码已使用
     */
    int markUsed(@Param("id") Long id, @Param("useTime") Date useTime);
}
