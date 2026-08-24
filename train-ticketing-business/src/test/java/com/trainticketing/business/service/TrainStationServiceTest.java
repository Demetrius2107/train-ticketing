package com.trainticketing.business.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trainticketing.business.domain.TrainStation;
import com.trainticketing.common.exception.BusinessException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * <p>Title: TrainStationServiceTest</p>
 * <p>Description: TrainStationService.validateTimeMonotonic 边界场景单测。
 * 该方法是纯逻辑（不依赖 DB），直接构造经停站列表与时刻参数验证。
 * 覆盖：空列表首站、追加末尾、插入中间、本站自洽、跨日、首末站空值。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-17
 * @updateTime 2026-08-17
 */
class TrainStationServiceTest {

    /** 构造一个经停站（只关心站序/到达/发车） */
    private TrainStation station(int index, String arrive, String leave) {
        TrainStation ts = new TrainStation();
        ts.setId((long) index);
        ts.setStationIndex(index);
        ts.setArriveTime(arrive == null ? null : java.time.LocalTime.parse(arrive));
        ts.setLeaveTime(leave == null ? null : java.time.LocalTime.parse(leave));
        return ts;
    }

    /**
     * 场景1：空列表插入首站。
     * 首站通常不填到达、只填发车，且没有前驱/后继，应通过。
     */
    @Test
    void insertFirstStationIntoEmptyListShouldPass() {
        assertThatCode(() -> TrainStationService.validateTimeMonotonic(
            new ArrayList<>(), 1, null, LocalTime.of(8, 0)))
            .doesNotThrowAnyException();
    }

    /**
     * 场景2：追加到末尾，时刻递增（前站 08:00 发、本站 08:50 到 08:55 发）。
     */
    @Test
    void appendToEndWithIncreasingTimeShouldPass() {
        List<TrainStation> existing = new ArrayList<>();
        existing.add(station(1, null, "08:00"));
        assertThatCode(() -> TrainStationService.validateTimeMonotonic(
            existing, 2, LocalTime.of(8, 50), LocalTime.of(8, 55)))
            .doesNotThrowAnyException();
    }

    /**
     * 场景3：追加到末尾，但本站到达早于前站发车（时刻倒退），应抛异常。
     * 前站 08:00 发，本站 07:30 到——车还没到就"到达"了，非法。
     */
    @Test
    void appendToEndWithBackwardTimeShouldThrow() {
        List<TrainStation> existing = new ArrayList<>();
        existing.add(station(1, null, "08:00"));
        assertThatThrownBy(() -> TrainStationService.validateTimeMonotonic(
            existing, 2, LocalTime.of(7, 30), LocalTime.of(7, 35)))
            .isInstanceOf(BusinessException.class);
    }

    /**
     * 场景4：插入中间，时刻合法。
     * 已有站序1(发08:00)、站序3(到10:00)，插入站序2(到08:50 发08:55)。
     */
    @Test
    void insertMiddleWithValidTimeShouldPass() {
        List<TrainStation> existing = new ArrayList<>();
        existing.add(station(1, null, "08:00"));
        existing.add(station(3, "10:00", null));
        assertThatCode(() -> TrainStationService.validateTimeMonotonic(
            existing, 2, LocalTime.of(8, 50), LocalTime.of(8, 55)))
            .doesNotThrowAnyException();
    }

    /**
     * 场景5：插入中间，但前驱站发车晚于本站到达。
     * 站序1发08:30，本站序2 到08:00——前站还没发，本站就到了，非法。
     */
    @Test
    void insertMiddleWithPrevLeaveAfterArriveShouldThrow() {
        List<TrainStation> existing = new ArrayList<>();
        existing.add(station(1, null, "08:30"));
        existing.add(station(3, "10:00", null));
        assertThatThrownBy(() -> TrainStationService.validateTimeMonotonic(
            existing, 2, LocalTime.of(8, 0), LocalTime.of(8, 5)))
            .isInstanceOf(BusinessException.class);
    }

    /**
     * 场景6：插入中间，但本站发车晚于后继站到达。
     * 本站序2 发09:30，后继站序3 到09:00——本站还没走，后站就到了，非法。
     */
    @Test
    void insertMiddleWithNextArriveBeforeLeaveShouldThrow() {
        List<TrainStation> existing = new ArrayList<>();
        existing.add(station(1, null, "08:00"));
        existing.add(station(3, "09:00", null));
        assertThatThrownBy(() -> TrainStationService.validateTimeMonotonic(
            existing, 2, LocalTime.of(8, 30), LocalTime.of(9, 30)))
            .isInstanceOf(BusinessException.class);
    }

    /**
     * 场景7：本站自洽——到达不早于发车（08:32 到、08:30 发）。
     */
    @Test
    void stationWithArriveAfterLeaveShouldThrow() {
        assertThatThrownBy(() -> TrainStationService.validateTimeMonotonic(
            new ArrayList<>(), 1, LocalTime.of(8, 32), LocalTime.of(8, 30)))
            .isInstanceOf(BusinessException.class);
    }

    /**
     * 场景8：跨日限制。当前 train_station 仅 time 字段、无日偏移标记，
     * 无法区分"同日内倒退"与"次日跨日"，故按同一日严格递增校验。
     * 前站 23:50 发车、本站次日 06:00 到，在当前模型下被判定为"倒退"（非法）。
     * 若将来支持跨日运行，需增加 day_offset 字段并改用绝对时间比较。
     */
    @Test
    void crossDayIsNotSupportedCurrentlyShouldThrow() {
        List<TrainStation> existing = new ArrayList<>();
        existing.add(station(1, null, "23:50"));
        assertThatThrownBy(() -> TrainStationService.validateTimeMonotonic(
            existing, 2, LocalTime.of(6, 0), LocalTime.of(6, 10)))
            .isInstanceOf(BusinessException.class);
    }

    /**
     * 场景9：同刻不允许（不严格递增）。前站 23:50 发车，本站 23:50 到达——同时刻，非法。
     * 覆盖 compareAfterDay 的 <= 边界：相等不算递增。
     */
    @Test
    void sameTimeAsPrevLeaveShouldThrow() {
        List<TrainStation> existing = new ArrayList<>();
        existing.add(station(1, null, "23:50"));
        assertThatThrownBy(() -> TrainStationService.validateTimeMonotonic(
            existing, 2, LocalTime.of(23, 50), LocalTime.of(23, 55)))
            .isInstanceOf(BusinessException.class);
    }

    /**
     * 场景10：首站不填到达、末站不填发车（空值守卫）。
     * 已有站序1(发08:00)，本站序2 只填到达不填发车（模拟"暂时未定发车"）。
     * 注：末站应不填发车，本站到达>前站发车即可，后继为空跳过。
     */
    @Test
    void lastStationWithoutLeaveTimeShouldPass() {
        List<TrainStation> existing = new ArrayList<>();
        existing.add(station(1, null, "08:00"));
        assertThatCode(() -> TrainStationService.validateTimeMonotonic(
            existing, 2, LocalTime.of(8, 50), null))
            .doesNotThrowAnyException();
    }

    @Test
    void firstStationOnlyLeaveTimeShouldPass() {
        List<TrainStation> existing = new ArrayList<>();
        existing.add(station(2, "08:50", "08:55"));
        // 站序1 作为首站：不填到达，只填发车 08:00，必须早于站序2 到达 08:50
        assertThatCode(() -> TrainStationService.validateTimeMonotonic(
            existing, 1, null, LocalTime.of(8, 0)))
            .doesNotThrowAnyException();
    }
}
