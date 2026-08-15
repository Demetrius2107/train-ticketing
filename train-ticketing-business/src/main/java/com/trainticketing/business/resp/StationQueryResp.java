package com.trainticketing.business.resp;

/**
 * <p>Title: StationQueryResp</p>
 * <p>Description: 车站查询结果</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class StationQueryResp {

    /** 主键ID */
    private Long id;

    /** 车站名称 */
    private String name;

    /** 车站拼音全拼 */
    private String namePinyin;

    /** 车站拼音简拼 */
    private String namePy;

    /** 所属城市 */
    private String city;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamePinyin() {
        return namePinyin;
    }

    public void setNamePinyin(String namePinyin) {
        this.namePinyin = namePinyin;
    }

    public String getNamePy() {
        return namePy;
    }

    public void setNamePy(String namePy) {
        this.namePy = namePy;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "StationQueryResp{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", namePinyin='" + namePinyin + '\'' +
            ", namePy='" + namePy + '\'' +
            ", city='" + city + '\'' +
            '}';
    }
}
