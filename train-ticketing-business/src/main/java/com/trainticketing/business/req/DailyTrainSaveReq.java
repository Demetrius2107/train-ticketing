package com.trainticketing.business.req;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * <p>Title: DailyTrainSaveReq</p>
 * <p>Description: 每日车次（排班）新增请求</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class DailyTrainSaveReq {

    /** 车次id */
    @NotNull(message = "[车次]不能为空")
    private Long trainId;

    /** 运行日期，不能早于今天（如 2026-08-20） */
    @NotNull(message = "[运行日期]不能为空")
    @FutureOrPresent(message = "[运行日期]不能早于今天")
    private LocalDate runDate;

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    @Override
    public String toString() {
        return "DailyTrainSaveReq{" +
            "trainId=" + trainId +
            ", runDate=" + runDate +
            '}';
    }
}
