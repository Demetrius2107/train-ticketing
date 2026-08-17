package com.trainticketing.member.resp;

public class MemberLoginResp {
  private Long id;

  private String mobile;

  /** 登录签发的 JWT，前端后续请求放 Authorization: Bearer {token} */
  private String token;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append(" [");
    sb.append("Hash = ").append(hashCode());
    sb.append(", id=").append(id);
    sb.append(", mobile=").append(mobile);
    sb.append("]");
    return sb.toString();
  }
}
