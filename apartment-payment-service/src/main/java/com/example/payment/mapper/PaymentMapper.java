package com.example.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 支付Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 根据支付单号查询
     */
    @Select("SELECT * FROM payment WHERE payment_no = #{paymentNo}")
    Payment selectByPaymentNo(@Param("paymentNo") String paymentNo);

    /**
     * 根据订单号和状态查询支付单列表
     */
    @Select("SELECT * FROM payment WHERE order_no = #{orderNo} AND payment_status = #{status}")
    List<Payment> selectListByOrderNoAndStatus(@Param("orderNo") String orderNo,
            @Param("status") String status);

    /**
     * 根据订单号查询所有支付记录
     */
    @Select("SELECT * FROM payment WHERE order_no = #{orderNo}")
    List<Payment> selectByOrderNo(@Param("orderNo") String orderNo);
}
