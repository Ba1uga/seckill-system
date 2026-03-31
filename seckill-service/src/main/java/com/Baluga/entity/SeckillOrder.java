package com.Baluga.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_seckill_order")
public class SeckillOrder {

    private Long id;

    private Long userId;

    private Long seckillProductId;

    private Long orderId;
}
