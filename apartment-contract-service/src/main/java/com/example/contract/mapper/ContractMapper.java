package com.example.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.contract.entity.Contract;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同Mapper
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {
}
