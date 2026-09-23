package com.example.order.lock;

import com.example.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 房源分布式锁工具类
 * 用于防止房源超卖
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HouseLock {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "lock:house:";

    /**
     * 获取房源锁的Key
     */
    public String getLockKey(Long houseId) {
        return LOCK_PREFIX + houseId;
    }

    /**
     * 尝试获取房源锁并执行操作
     *
     * @param houseId   房源ID
     * @param waitTime  等待时间
     * @param leaseTime 锁持有时间
     * @param timeUnit  时间单位
     * @param supplier  要执行的操作
     * @return 操作结果
     * @throws Exception 获取锁失败或操作执行异常
     */
    public <T> T executeWithLock(Long houseId, long waitTime, long leaseTime,
            TimeUnit timeUnit, Supplier<T> supplier) throws Exception {
        String lockKey = getLockKey(houseId);
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            // 尝试获取锁
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                log.warn("获取房源锁失败，房源可能正在被其他用户操作: houseId={}", houseId);
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            log.info("成功获取房源锁: houseId={}, thread={}", houseId, Thread.currentThread().getName());

            // 执行操作
            return supplier.get();

        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放房源锁: houseId={}", houseId);
            }
        }
    }

    /**
     * 尝试获取房源锁并执行操作（无返回值）
     */
    public void executeWithLock(Long houseId, long waitTime, long leaseTime,
            TimeUnit timeUnit, Runnable runnable) throws Exception {
        executeWithLock(houseId, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 使用默认参数获取房源锁并执行操作
     * 等待时间 0 秒（立刻失败模式）：抢购场景标准做法
     * 拿不到锁立刻返回"系统繁忙"，避免线程积压导致雪崩
     */
    public <T> T executeWithLockDefault(Long houseId, Supplier<T> supplier) throws Exception {
        return executeWithLock(houseId, 0, 10, TimeUnit.SECONDS, supplier);
    }

    /**
     * 使用默认参数获取房源锁并执行操作（无返回值）
     */
    public void executeWithLockDefault(Long houseId, Runnable runnable) throws Exception {
        executeWithLockDefault(houseId, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 检查房源是否被锁定
     */
    public boolean isLocked(Long houseId) {
        String lockKey = getLockKey(houseId);
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }
}
