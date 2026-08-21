package com.trainticketing.member.domain;

import java.util.Date;

/**
 * <p>Title: Passenger</p>
 * <p>Description: 乘车人实体，与 passenger 表 1:1 对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
public class Passenger {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 会员id
     */
    private Long memberId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 旅客类型|枚举[PassengerTypeEnum]: 1成人 2儿童 3学生
     */
    private String type;

    /**
     * 新增时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "Passenger{" +
                "id=" + id +
                ", memberId=" + memberId +
                ", name='" + name + '\'' +
                ", idCard='" + idCard + '\'' +
                ", type='" + type + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
