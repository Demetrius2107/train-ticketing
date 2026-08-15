package com.trainticketing.business.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p>Title: StationSaveReq</p>
 * <p>Description: 车站新增请求</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class StationSaveReq {

    /** 车站名称（唯一） */
    @NotBlank(message = "[车站名称]不能为空")
    @Size(max = 50, message = "[车站名称]最长50个字符")
    private String name;

    /** 车站拼音全拼 */
    @NotBlank(message = "[车站拼音全拼]不能为空")
    @Size(max = 100, message = "[车站拼音全拼]最长100个字符")
    private String namePinyin;

    /** 车站拼音简拼 */
    @NotBlank(message = "[车站拼音简拼]不能为空")
    @Size(max = 50, message = "[车站拼音简拼]最长50个字符")
    private String namePy;

    /** 所属城市 */
    @Size(max = 50, message = "[所属城市]最长50个字符")
    private String city;

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
        return "StationSaveReq{" +
            "name='" + name + '\'' +
            ", namePinyin='" + namePinyin + '\'' +
            ", namePy='" + namePy + '\'' +
            ", city='" + city + '\'' +
            '}';
    }
}
