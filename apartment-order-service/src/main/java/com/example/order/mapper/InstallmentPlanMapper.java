package com.example.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.order.entity.InstallmentPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 分期付款计划Mapper
 */
@Mapper
public interface InstallmentPlanMapper extends BaseMapper<InstallmentPlan> {

    /**
     * 根据订单号查询所有分期计划
     */
    @Select("SELECT * FROM installment_plan WHERE order_no = #{orderNo} ORDER BY period_no")
    List<InstallmentPlan> selectListByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询订单的下一期未支付账单
     */
    @Select("SELECT * FROM installment_plan " +
            "WHERE order_no = #{orderNo} AND payment_status = 'UNPAID' " +
            "ORDER BY period_no LIMIT 1")
    InstallmentPlan selectOneUnpaidByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 更新分期计划状态
     */
    @Update("UPDATE installment_plan SET payment_status = #{status} WHERE order_no = #{orderNo}")
    int updateStatusByOrderNo(@Param("orderNo") String orderNo, @Param("status") String status);
}
