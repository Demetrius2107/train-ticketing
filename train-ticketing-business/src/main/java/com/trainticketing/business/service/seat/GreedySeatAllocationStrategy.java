package com.trainticketing.business.service.seat;

import cn.hutool.core.collection.CollUtil;
import com.trainticketing.business.domain.DailyTrainSeat;
import com.trainticketing.business.enums.SeatTypeEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * <p>Title: GreedySeatAllocationStrategy</p>
 * <p>Description: 贪心选座策略（默认实现）。优先级：
 * <ol>
 *   <li>同排相邻：N 人坐同一排（seatIndex 相同），座位利用率最高、体验最好</li>
 *   <li>同车厢连续：同排放不下，退而在同一车厢内取连续座位</li>
 *   <li>跨车厢兜底：一个车厢凑不齐，跨车厢按序取前 N</li>
 * </ol>
 * 卧铺下铺优先：铺位标签序越大越靠下（U上/M中/D下），选座时优先下铺。
 * 普通座靠窗优先：标签序越小越靠窗（A），选座时优先靠窗。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-17
 * @updateTime 2026-08-17
 * @since 1.0
 */
@Component
public class GreedySeatAllocationStrategy implements SeatAllocationStrategy {

    @Override
    public List<DailyTrainSeat> allocate(List<DailyTrainSeat> availableSeats, String seatType, int need) {
        if (CollUtil.isEmpty(availableSeats) || availableSeats.size() < need) {
            return null;
        }
        if (need == 1) {
            // 单人直接取最优一个（卧铺下铺优先 / 普通座靠窗优先）
            return List.of(pickBestSingle(availableSeats, seatType));
        }
        SeatTypeEnum typeEnum = SeatTypeEnum.getByCode(seatType);
        // 1. 同排相邻：按车厢→排分组，找一排能放下 need 个的
        List<DailyTrainSeat> sameRow = trySameRow(availableSeats, typeEnum, need);
        if (sameRow != null) {
            return sameRow;
        }
        // 2. 同车厢连续：找第一个能凑齐 need 个的车厢
        List<DailyTrainSeat> sameCarriage = trySameCarriage(availableSeats, typeEnum, need);
        if (sameCarriage != null) {
            return sameCarriage;
        }
        // 3. 跨车厢兜底：全部按序取前 need 个
        return pickFirstN(availableSeats, typeEnum, need);
    }

    /**
     * 尝试同排相邻：按车厢→排分组，找第一排可售数 >= need 的，从中选 need 个。
     */
    private List<DailyTrainSeat> trySameRow(List<DailyTrainSeat> seats, SeatTypeEnum typeEnum, int need) {
        // 按 carriageId → seatIndex 分组，保留输入顺序（已按车厢/排/标签排序）
        Map<Long, Map<Integer, List<DailyTrainSeat>>> grouped = groupByCarriageAndRow(seats);
        for (Map.Entry<Long, Map<Integer, List<DailyTrainSeat>>> carriageEntry : grouped.entrySet()) {
            for (Map.Entry<Integer, List<DailyTrainSeat>> rowEntry : carriageEntry.getValue().entrySet()) {
                List<DailyTrainSeat> rowSeats = rowEntry.getValue();
                if (rowSeats.size() >= need) {
                    return pickFromRow(rowSeats, typeEnum, need);
                }
            }
        }
        return null;
    }

    /**
     * 尝试同车厢连续：找第一个可售数 >= need 的车厢，从中按排序取 need 个。
     */
    private List<DailyTrainSeat> trySameCarriage(List<DailyTrainSeat> seats, SeatTypeEnum typeEnum, int need) {
        Map<Long, List<DailyTrainSeat>> byCarriage = groupByCarriage(seats);
        for (Map.Entry<Long, List<DailyTrainSeat>> entry : byCarriage.entrySet()) {
            if (entry.getValue().size() >= need) {
                return pickFromCarriage(entry.getValue(), typeEnum, need);
            }
        }
        return null;
    }

    /**
     * 从一排座位中选 need 个：卧铺下铺优先，普通座靠窗优先。
     */
    private List<DailyTrainSeat> pickFromRow(List<DailyTrainSeat> rowSeats, SeatTypeEnum typeEnum, int need) {
        List<DailyTrainSeat> sorted = sortForPick(rowSeats, typeEnum);
        return new ArrayList<>(sorted.subList(0, need));
    }

    /**
     * 从一个车厢的可售座位中选 need 个：按排序优先同排，不足跨排补齐。
     */
    private List<DailyTrainSeat> pickFromCarriage(List<DailyTrainSeat> carriageSeats, SeatTypeEnum typeEnum, int need) {
        // 车厢内按排分组，逐排取直到凑齐
        Map<Integer, List<DailyTrainSeat>> byRow = new LinkedHashMap<>();
        for (DailyTrainSeat s : carriageSeats) {
            byRow.computeIfAbsent(s.getSeatIndex(), k -> new ArrayList<>()).add(s);
        }
        List<DailyTrainSeat> picked = new ArrayList<>(need);
        for (List<DailyTrainSeat> row : byRow.values()) {
            List<DailyTrainSeat> sorted = sortForPick(row, typeEnum);
            for (DailyTrainSeat s : sorted) {
                picked.add(s);
                if (picked.size() == need) {
                    return picked;
                }
            }
        }
        return picked;
    }

    /**
     * 跨车厢兜底：全部座位按车厢→排→标签优先级取前 need 个。
     */
    private List<DailyTrainSeat> pickFirstN(List<DailyTrainSeat> seats, SeatTypeEnum typeEnum, int need) {
        List<DailyTrainSeat> sorted = sortForPick(seats, typeEnum);
        return new ArrayList<>(sorted.subList(0, need));
    }

    /**
     * 单人选座：取最优一个。
     */
    private DailyTrainSeat pickBestSingle(List<DailyTrainSeat> seats, String seatType) {
        SeatTypeEnum typeEnum = SeatTypeEnum.getByCode(seatType);
        return sortForPick(seats, typeEnum).get(0);
    }

    /**
     * 排序供挑选：卧铺按下铺优先（label 序降序），普通座靠窗优先（label 序升序）。
     * 同优先级内按车厢→排稳定排序。
     */
    private List<DailyTrainSeat> sortForPick(List<DailyTrainSeat> seats, SeatTypeEnum typeEnum) {
        List<DailyTrainSeat> copy = new ArrayList<>(seats);
        Comparator<DailyTrainSeat> labelCmp;
        if (typeEnum != null && typeEnum.isSleeper()) {
            // 卧铺：下铺优先，label 在 seatLabels 中序号越大越优先
            List<String> labels = typeEnum.getSeatLabels();
            labelCmp = Comparator.comparingInt((DailyTrainSeat s) -> -labels.indexOf(s.getSeatLabel()));
        } else {
            // 普通座：靠窗优先，label 序号越小越优先
            List<String> labels = typeEnum == null ? List.of() : typeEnum.getSeatLabels();
            labelCmp = Comparator.comparingInt((DailyTrainSeat s) -> labels.indexOf(s.getSeatLabel()));
        }
        copy.sort(labelCmp
                .thenComparingLong(DailyTrainSeat::getCarriageId)
                .thenComparingInt(DailyTrainSeat::getSeatIndex));
        return copy;
    }

    /**
     * 按车厢分组，保留输入顺序（LinkedHashMap 保序）
     */
    private Map<Long, List<DailyTrainSeat>> groupByCarriage(List<DailyTrainSeat> seats) {
        Map<Long, List<DailyTrainSeat>> byCarriage = new LinkedHashMap<>();
        for (DailyTrainSeat s : seats) {
            byCarriage.computeIfAbsent(s.getCarriageId(), k -> new ArrayList<>()).add(s);
        }
        return byCarriage;
    }

    /**
     * 按车厢→排两级分组，保留输入顺序
     */
    private Map<Long, Map<Integer, List<DailyTrainSeat>>> groupByCarriageAndRow(List<DailyTrainSeat> seats) {
        Map<Long, Map<Integer, List<DailyTrainSeat>>> grouped = new LinkedHashMap<>();
        for (DailyTrainSeat s : seats) {
            grouped.computeIfAbsent(s.getCarriageId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(s.getSeatIndex(), k -> new ArrayList<>()).add(s);
        }
        return grouped;
    }
}
