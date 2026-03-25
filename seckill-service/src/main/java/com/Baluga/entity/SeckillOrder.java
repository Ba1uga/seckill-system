package com.Baluga.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("t_seckill_order")
@Schema(name = "SeckillOrder", description = "秒杀订单")
public class SeckillOrder {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "秒杀商品ID")
    private Long seckillProductId;

    @Schema(description = "订单ID")
    private Long orderId;
}