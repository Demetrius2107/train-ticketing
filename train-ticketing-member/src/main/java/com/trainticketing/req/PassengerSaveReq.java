package com.trainticketing.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>Title: PassengerSaveReq</p>
 * <p>Description: 乘车人新增请求</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class PassengerSaveReq {

    /** 会员id */
    @NotNull(message = "[会员id]不能为空")
    private Long memberId;

    /** 姓名 */
    @NotBlank(message = "[姓名]不能为空")
    private String name;

    /** 身份证号 */
    @NotBlank(message = "[身份证号]不能为空")
    private String idCard;

    /** 旅客类型|枚举[PassengerTypeEnum]: 1成人 2儿童 3学生 */
    @NotBlank(message = "[旅客类型]不能为空")
    private String type;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "PassengerSaveReq{" +
            "memberId=" + memberId +
            ", name='" + name + '\'' +
            ", idCard='" + idCard + '\'' +
            ", type='" + type + '\'' +
            '}';
    }
}
