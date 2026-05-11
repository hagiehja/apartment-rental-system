package com.example.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payment.entity.AccountTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户流水Mapper
 */
@Mapper
public interface AccountTransactionMapper extends BaseMapper<AccountTransaction> {
}
