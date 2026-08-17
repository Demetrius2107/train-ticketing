package com.trainticketing.business.config;

/**
 * <p>Title: TicketLockConfig</p>
 * <p>Description: 余票分布式锁配置常量。
 * 阶段2 防超卖：下单按 排班ID+座位类型 加 Redisson 锁，锁粒度细到座位类型，
 * 不同座位类型可并行下单，同座位类型串行化保证选座原子性。
 * RedissonClient 由 redisson-spring-boot-starter 读取 spring.data.redis.* 自动装配。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-17
 * @updateTime 2026-08-17
 */
public final class TicketLockConfig {

    private TicketLockConfig() {
    }

    /** 锁 key 前缀：ticket:lock:{dailyTrainId}:{seatType} */
    public static final String LOCK_KEY_PREFIX = "ticket:lock:";

    /** 等待获取锁的最大时间（秒）：春运抢票场景快速失败，避免长堆压 */
    public static final long LOCK_WAIT_SECONDS = 3;

    /** 锁持有时间（秒）：覆盖一次下单事务 + 缓存操作，超时自动释放防死锁 */
    public static final long LOCK_LEASE_SECONDS = 10;

    /**
     * 构建下单分布式锁 key
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @return lock key
     */
    public static String lockKey(Long dailyTrainId, String seatType) {
        return LOCK_KEY_PREFIX + dailyTrainId + ":" + seatType;
    }
}
