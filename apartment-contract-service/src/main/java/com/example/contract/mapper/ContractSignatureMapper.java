package com.example.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.contract.entity.ContractSignature;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同签署记录Mapper
 */
@Mapper
public interface ContractSignatureMapper extends BaseMapper<ContractSignature> {
}
