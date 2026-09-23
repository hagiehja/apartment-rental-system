package com.example.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.order.entity.RentalOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<RentalOrder> {

    /**
     * 根据订单号查询订单
     */
    @Select("SELECT * FROM rental_order WHERE order_no = #{orderNo}")
    RentalOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询超时未支付订单
     * 使用数据库的NOW()函数确保时区一致性
     */
    @Select("SELECT * FROM rental_order " +
            "WHERE order_status = 'PENDING_PAYMENT' " +
            "AND expire_time < NOW() " +
            "LIMIT 100")
    List<RentalOrder> selectExpiredPendingOrders();

    /**
     * 获取数据库当前时间加30分钟作为过期时间
     * 确保与超时检测使用相同的时间源
     */
    @Select("SELECT DATE_ADD(NOW(), INTERVAL 30 MINUTE)")
    LocalDateTime getExpireTime();
}
