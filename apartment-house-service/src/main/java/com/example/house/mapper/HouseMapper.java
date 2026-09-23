package com.example.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.house.entity.House;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房源Mapper接口
 */
@Mapper
public interface HouseMapper extends BaseMapper<House> {
}
