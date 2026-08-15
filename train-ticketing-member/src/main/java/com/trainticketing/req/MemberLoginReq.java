package com.trainticketing.req;

import jakarta.validation.constraints.NotBlank;

public class MemberLoginReq {


  @NotBlank(message = "[手机号]不能为空")
  private String mobile;

  @NotBlank(message = "[短信验证码]不能为空")
  private String code;

  public String getCode() {
    return code;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("MemberLoginReq{");
    sb.append("mobile='").append(mobile).append('\'');
    sb.append(", code='").append(code).append('\'');
    sb.append('}');
    return sb.toString();
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

}
