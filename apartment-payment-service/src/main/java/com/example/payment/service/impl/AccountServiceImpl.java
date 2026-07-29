package com.example.payment.service.impl;

import com.example.payment.dto.AccountBalanceDTO;
import com.example.payment.entity.AccountTransaction;
import com.example.payment.entity.UserAccount;
import com.example.payment.enums.TransactionType;
import com.example.common.exception.BusinessException;
import com.example.payment.mapper.AccountTransactionMapper;
import com.example.payment.mapper.UserAccountMapper;
import com.example.payment.service.AccountService;
import com.example.payment.utils.PaymentNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 账户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserAccountMapper accountMapper;
    private final AccountTransactionMapper transactionMapper;
    private final RedissonClient redissonClient;

    @Override
    public AccountBalanceDTO getBalance(Long userId) {
        UserAccount account = accountMapper.selectByUserId(userId);

        if (account == null) {
            // 用户首次查询，初始化账户
            account = initializeAccount(userId);
        }

        AccountBalanceDTO dto = new AccountBalanceDTO();
        dto.setUserId(account.getUserId());
        dto.setBalance(account.getBalance());
        dto.setFrozenAmount(account.getFrozenAmount());
        dto.setTotalIncome(account.getTotalIncome());
        dto.setTotalExpense(account.getTotalExpense());
        return dto;
    }

    @Override
    @Transactional
    public void deductBalance(Long userId, BigDecimal amount, String relatedNo, String remark) {
        // 【分布式锁】双重保护：分布式锁 + 数据库行锁
        String lockKey = "account:deduct:lock:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待5秒，锁自动释放时间10秒
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("账户操作中，请稍后再试");
            }

            // 1. 加行锁查询账户
            UserAccount account = accountMapper.selectByUserIdForUpdate(userId);

            if (account == null) {
                // 用户首次支付，初始化账户
                account = initializeAccount(userId);
                account = accountMapper.selectByUserIdForUpdate(userId);
            }

            // 2. 检查余额是否充足
            if (account.getBalance().compareTo(amount) < 0) {
                throw new BusinessException("余额不足，当前余额：" + account.getBalance());
            }

            // 3. 扣减余额
            BigDecimal balanceBefore = account.getBalance();
            account.setBalance(account.getBalance().subtract(amount));
            account.setTotalExpense(account.getTotalExpense().add(amount));
            accountMapper.updateById(account);

            // 4. 记录流水
            recordTransaction(userId, amount.negate(), TransactionType.PAYMENT,
                    balanceBefore, account.getBalance(), relatedNo, remark);

            log.info("扣款成功，userId={}, amount={}, balance={}", userId, amount, account.getBalance());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取账户扣款锁失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void refundBalance(Long userId, BigDecimal amount, String relatedNo, String remark) {
        // 1. 加行锁查询账户
        UserAccount account = accountMapper.selectByUserIdForUpdate(userId);

        if (account == null) {
            throw new BusinessException("账户不存在");
        }

        // 2. 增加余额
        BigDecimal balanceBefore = account.getBalance();
        account.setBalance(account.getBalance().add(amount));
        accountMapper.updateById(account);

        // 3. 记录流水
        recordTransaction(userId, amount, TransactionType.REFUND,
                balanceBefore, account.getBalance(), relatedNo, remark);

        log.info("退款成功，userId={}, amount={}, balance={}", userId, amount, account.getBalance());
    }

    @Override
    @Transactional
    public void receiveIncome(Long landlordId, BigDecimal amount, String relatedNo, String remark) {
        // 1. 加行锁查询账户
        UserAccount account = accountMapper.selectByUserIdForUpdate(landlordId);

        if (account == null) {
            account = initializeAccount(landlordId);
            account = accountMapper.selectByUserIdForUpdate(landlordId);
        }

        // 2. 增加余额和收入
        BigDecimal balanceBefore = account.getBalance();
        account.setBalance(account.getBalance().add(amount));
        account.setTotalIncome(account.getTotalIncome().add(amount));
        accountMapper.updateById(account);

        // 3. 记录流水
        recordTransaction(landlordId, amount, TransactionType.INCOME,
                balanceBefore, account.getBalance(), relatedNo, remark);

        log.info("收款成功，landlordId={}, amount={}, balance={}", landlordId, amount, account.getBalance());
    }

    /**
     * 初始化账户
     */
    @Transactional
    public UserAccount initializeAccount(Long userId) {
        UserAccount account = new UserAccount();
        account.setUserId(userId);
        account.setBalance(new BigDecimal("100000.00")); // 初始余额10万
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setTotalIncome(BigDecimal.ZERO);
        account.setTotalExpense(BigDecimal.ZERO);
        accountMapper.insert(account);

        log.info("账户初始化成功，userId={}, balance={}", userId, account.getBalance());
        return account;
    }

    /**
     * 记录流水
     */
    private void recordTransaction(Long userId, BigDecimal amount, TransactionType type,
            BigDecimal balanceBefore, BigDecimal balanceAfter,
            String relatedNo, String remark) {
        AccountTransaction transaction = new AccountTransaction();
        transaction.setTransactionNo(PaymentNoGenerator.generateTransactionNo());
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setTransactionType(type.name());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRelatedNo(relatedNo);
        transaction.setRemark(remark);
        transactionMapper.insert(transaction);
    }

    @Override
    public java.util.List<AccountTransaction> getTransactionList(Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AccountTransaction> query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        query.eq(AccountTransaction::getUserId, userId);
        query.orderByDesc(AccountTransaction::getCreateTime);
        return transactionMapper.selectList(query);
    }
}
