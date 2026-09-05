package com.trainticketing.business.service;

import com.trainticketing.business.config.BusinessApplication;
import com.trainticketing.business.req.DailyTrainSaveReq;
import com.trainticketing.business.req.OrderSaveReq;
import com.trainticketing.business.req.StationSaveReq;
import com.trainticketing.business.req.TrainCarriageSaveReq;
import com.trainticketing.business.req.TrainPriceSaveReq;
import com.trainticketing.business.req.TrainSaveReq;
import com.trainticketing.business.req.TrainStationSaveReq;
import com.trainticketing.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Title: OrderConcurrencyTest</p>
 * <p>Description: 下单并发正确性测试（需本地 MySQL + Redis，见根目录 docker-compose.yml）。
 * 真实 Spring 上下文直调 OrderService，多线程同起点放行制造竞争，断言 DB/Redis 终态：
 * 1. 冒烟：单线程下单后各层状态正确；
 * 2. 防超卖：N 并发抢 M 张票，成票数精确等于库存；
 * 3. 跨区间重叠：A-C / A-B / B-C 并发，相邻段占用不超库存，缓存与 DB 终态一致。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-01
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = BusinessApplication.class)
@DisplayName("下单并发正确性")
class OrderConcurrencyTest {

    /** 座位类型：二等座（SeatTypeEnum 编码 3，每排 5 座 A B C D F） */
    private static final String SEAT_TYPE = "3";
    /** 本次测试的座位库存（一节车厢） */
    private static final int STOCK = 10;
    /** 并发线程数 */
    private static final int THREADS = 100;

    @Resource
    private StationService stationService;
    @Resource
    private TrainService trainService;
    @Resource
    private TrainStationService trainStationService;
    @Resource
    private TrainCarriageService trainCarriageService;
    @Resource
    private TrainSeatService trainSeatService;
    @Resource
    private TrainPriceService trainPriceService;
    @Resource
    private DailyTrainService dailyTrainService;
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;
    @Resource
    private TicketCacheService ticketCacheService;
    @Resource
    private OrderService orderService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    private String suffix;
    private Long stationAId;
    private Long stationBId;
    private Long stationCId;
    private Long trainId;
    private Long dailyTrainId;
    private LocalDate runDate;

    /**
     * 造一条完整数据链：3 站（站序 1/2/3）→ 车次 → 经停站 → 车厢 → 座位档案 → 票价
     * → 每日排班 → 当日座位 + 余票缓存初始化。库存 = {@link #STOCK} 张二等座。
     */
    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.currentTimeMillis() % 100_000_000L);
        runDate = LocalDate.now().plusDays(7);

        stationAId = saveStation("北京西");
        stationBId = saveStation("石家庄");
        stationCId = saveStation("郑州东");

        TrainSaveReq train = new TrainSaveReq();
        train.setCode("T" + suffix);
        train.setType("1");
        train.setStartStationId(stationAId);
        train.setEndStationId(stationCId);
        train.setStartTime("08:00");
        train.setEndTime("09:00");
        trainId = trainService.save(train);

        saveTrainStation(1, stationAId, null, "08:00");
        saveTrainStation(2, stationBId, "08:30", "08:32");
        saveTrainStation(3, stationCId, "09:00", null);

        TrainCarriageSaveReq carriage = new TrainCarriageSaveReq();
        carriage.setTrainId(trainId);
        carriage.setCarriageIndex(1);
        carriage.setSeatType(SEAT_TYPE);
        carriage.setSeatCount(STOCK);
        Long carriageId = trainCarriageService.save(carriage);
        trainSeatService.generate(carriageId);

        TrainPriceSaveReq price = new TrainPriceSaveReq();
        price.setTrainId(trainId);
        price.setSeatType(SEAT_TYPE);
        price.setPrice(new BigDecimal("100.00"));
        trainPriceService.save(price);

        DailyTrainSaveReq daily = new DailyTrainSaveReq();
        daily.setTrainId(trainId);
        daily.setRunDate(runDate);
        dailyTrainId = dailyTrainService.save(daily);
        assertEquals(STOCK, dailyTrainSeatService.generate(dailyTrainId));
    }

    /**
     * 逆序清理本次造的数据与订单/缓存，测试间完全隔离。
     * 注意：不能用 @Transactional 回滚——子线程各自提交事务，主线程回滚管不到。
     */
    @AfterEach
    void tearDown() {
        if (dailyTrainId != null) {
            jdbcTemplate.update("DELETE FROM train_order_item WHERE order_id IN "
                    + "(SELECT id FROM train_order WHERE daily_train_id = ?)", dailyTrainId);
            jdbcTemplate.update("DELETE FROM train_order WHERE daily_train_id = ?", dailyTrainId);
            jdbcTemplate.update("DELETE FROM daily_train_seat WHERE daily_train_id = ?", dailyTrainId);
            ticketCacheService.deleteRemaining(dailyTrainId, SEAT_TYPE);
            jdbcTemplate.update("DELETE FROM daily_train WHERE id = ?", dailyTrainId);
        }
        if (trainId != null) {
            jdbcTemplate.update("DELETE FROM train_seat WHERE train_id = ?", trainId);
            jdbcTemplate.update("DELETE FROM train_carriage WHERE train_id = ?", trainId);
            jdbcTemplate.update("DELETE FROM train_price WHERE train_id = ?", trainId);
            jdbcTemplate.update("DELETE FROM train_station WHERE train_id = ?", trainId);
            jdbcTemplate.update("DELETE FROM train WHERE id = ?", trainId);
        }
        for (Long stationId : Arrays.asList(stationAId, stationBId, stationCId)) {
            if (stationId != null) {
                jdbcTemplate.update("DELETE FROM station WHERE id = ?", stationId);
            }
        }
    }

    @Test
    @DisplayName("冒烟：单线程下单成功，DB 与缓存终态正确")
    void smokeOrderSuccess() {
        String orderNo = orderService.save(buildOrder(1L, stationAId, stationCId));
        assertTrue(StringUtils.hasText(orderNo));

        // 跨区间 A-C 扣 1 张：路径上 A-B、B-C 两段各扣 1，A-C 整段余票 -1
        assertEquals(STOCK - 1, ticketCacheService.getRemaining(dailyTrainId, SEAT_TYPE, 1, 2));
        assertEquals(STOCK - 1, ticketCacheService.getRemaining(dailyTrainId, SEAT_TYPE, 2, 3));
        assertEquals(STOCK - 1, ticketCacheService.getRemaining(dailyTrainId, SEAT_TYPE, 1, 3));

        assertEquals(1, countOrders());
        assertEquals(1, countOrderItems());
        // sale_status 目前是虚设字段（issue 清单 #5）：占用关系完全由 train_order_item 记录，
        // 座位不能整体置"已售"——否则非重叠区间无法复用同一座位
        assertEquals(0, countSoldSeats());
    }

    @Test
    @DisplayName("防超卖：100 并发抢 10 张票，成票数精确等于库存且缓存与 DB 一致")
    void concurrentNoOversell() throws Exception {
        List<OrderSaveReq> reqList = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            reqList.add(buildOrder(900_000_000L + i, stationAId, stationCId));
        }
        List<Attempt> results = runConcurrent(reqList);

        long success = results.stream().filter(Attempt::success).count();
        results.stream().filter(a -> !a.success())
                .forEach(a -> assertInstanceOf(BusinessException.class, a.error(),
                        "出现非业务异常说明下单链路本身有问题"));
        assertEquals(STOCK, success, "成功数必须精确等于库存（失败原因允许为余票不足/锁忙）");

        assertEquals(0, ticketCacheService.getRemaining(dailyTrainId, SEAT_TYPE, 1, 2));
        assertEquals(0, ticketCacheService.getRemaining(dailyTrainId, SEAT_TYPE, 2, 3));
        assertEquals(0, ticketCacheService.getRemaining(dailyTrainId, SEAT_TYPE, 1, 3));
        assertEquals(STOCK, countOrders());
        assertEquals(STOCK, countOrderItems());
        // sale_status 虚设（见冒烟用例注释），占用由 train_order_item 表达
        assertEquals(0, countSoldSeats());
    }

    @Test
    @DisplayName("跨区间重叠：A-C/A-B/B-C 并发，相邻段占用不超库存且缓存与 DB 一致")
    void concurrentCrossInterval() throws Exception {
        List<OrderSaveReq> reqList = new ArrayList<>(THREADS);
        for (int i = 0; i < 40; i++) {
            reqList.add(buildOrder(800_000_000L + i, stationAId, stationCId));
        }
        for (int i = 0; i < 30; i++) {
            reqList.add(buildOrder(910_000_000L + i, stationAId, stationBId));
        }
        for (int i = 0; i < 30; i++) {
            reqList.add(buildOrder(920_000_000L + i, stationBId, stationCId));
        }
        List<Attempt> results = runConcurrent(reqList);

        results.stream().filter(a -> !a.success())
                .forEach(a -> assertInstanceOf(BusinessException.class, a.error(),
                        "出现非业务异常说明下单链路本身有问题"));
        long sAC = countSuccess(results, stationAId, stationCId);
        long sAB = countSuccess(results, stationAId, stationBId);
        long sBC = countSuccess(results, stationBId, stationCId);

        // 区间占用不变式：A-B 段占用 = A-B + A-C 成票数；B-C 段占用 = B-C + A-C 成票数
        assertTrue(sAC <= STOCK);
        assertTrue(sAB + sAC <= STOCK, "A-B 段超卖");
        assertTrue(sBC + sAC <= STOCK, "B-C 段超卖");

        // 缓存终态 = 库存 - 段占用（缓存与成票结果严格一致）
        assertEquals((int) (STOCK - sAB - sAC), ticketCacheService.getRemaining(dailyTrainId, SEAT_TYPE, 1, 2));
        assertEquals((int) (STOCK - sBC - sAC), ticketCacheService.getRemaining(dailyTrainId, SEAT_TYPE, 2, 3));
        // 整区间余票 = 路径上相邻子段余票的最小值（A-B 售罄则 A-C 也不可售）
        assertEquals((int) Math.min(STOCK - sAB - sAC, STOCK - sBC - sAC),
                ticketCacheService.getRemaining(dailyTrainId, SEAT_TYPE, 1, 3));

        // DB 终态与成票数一致；同一座位可被不重叠区间复用（如 A-B + B-C），故已售座位数 ≤ 库存
        long total = sAB + sBC + sAC;
        assertEquals(total, countOrders());
        assertEquals(total, countOrderItems());
        assertTrue(countSoldSeats() <= STOCK);
    }

    // ===== 造数据 =====

    private Long saveStation(String name) {
        StationSaveReq req = new StationSaveReq();
        req.setName("TEST-" + name + suffix);
        req.setNamePinyin("test" + suffix + name);
        req.setNamePy("TT" + suffix);
        req.setCity("TEST-城市" + suffix);
        return stationService.save(req);
    }

    private void saveTrainStation(int index, Long stationId, String arriveTime, String leaveTime) {
        TrainStationSaveReq req = new TrainStationSaveReq();
        req.setTrainId(trainId);
        req.setStationId(stationId);
        req.setStationIndex(index);
        req.setArriveTime(arriveTime);
        req.setLeaveTime(leaveTime);
        trainStationService.save(req);
    }

    private OrderSaveReq buildOrder(long memberId, Long departStationId, Long arriveStationId) {
        OrderSaveReq req = new OrderSaveReq();
        req.setMemberId(memberId);
        req.setIdempotentKey(UUID.randomUUID().toString());
        req.setDailyTrainId(dailyTrainId);
        req.setDepartStationId(departStationId);
        req.setArriveStationId(arriveStationId);
        req.setRunDate(runDate);
        req.setSeatType(SEAT_TYPE);
        OrderSaveReq.PassengerReq passenger = new OrderSaveReq.PassengerReq();
        passenger.setPassengerId(memberId);
        passenger.setName("压测乘客" + memberId);
        passenger.setIdCard("11010119900101" + String.format("%04d", (int) (memberId % 10000)));
        req.setPassengers(List.of(passenger));
        return req;
    }

    // ===== 并发工具 =====

    /**
     * 一次下单尝试的结果
     */
    private record Attempt(boolean success, long departId, long arriveId, String orderNo, Exception error) {
    }

    /**
     * 多线程同起点并发执行下单：所有线程先在 startGate 等待，统一放行制造真实竞争窗口。
     * 全部完成后统一返回结果列表；加超时防止锁死等异常挂住测试。
     */
    private List<Attempt> runConcurrent(List<OrderSaveReq> reqList) throws InterruptedException {
        int threads = reqList.size();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(threads);
        List<Attempt> results = Collections.synchronizedList(new ArrayList<>(threads));
        for (OrderSaveReq req : reqList) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    String orderNo = orderService.save(req);
                    results.add(new Attempt(true, req.getDepartStationId(), req.getArriveStationId(), orderNo, null));
                } catch (Exception e) {
                    results.add(new Attempt(false, req.getDepartStationId(), req.getArriveStationId(), null, e));
                } finally {
                    allDone.countDown();
                }
            });
        }
        startGate.countDown();
        assertTrue(allDone.await(120, TimeUnit.SECONDS), "并发线程未全部完成，疑似死锁或锁等待异常");
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        return results;
    }

    private long countSuccess(List<Attempt> results, long departId, long arriveId) {
        return results.stream()
                .filter(a -> a.success() && a.departId() == departId && a.arriveId() == arriveId)
                .count();
    }

    private int count(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }

    private int countOrders() {
        return count("SELECT COUNT(*) FROM train_order WHERE daily_train_id = ?", dailyTrainId);
    }

    private int countOrderItems() {
        return count("SELECT COUNT(*) FROM train_order_item i "
                + "JOIN train_order o ON i.order_id = o.id WHERE o.daily_train_id = ?", dailyTrainId);
    }

    private int countSoldSeats() {
        return count("SELECT COUNT(*) FROM daily_train_seat "
                + "WHERE daily_train_id = ? AND sale_status = '1'", dailyTrainId);
    }
}
