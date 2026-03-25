package com.Baluga.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_order")
@Schema(description = "订单表")
public class Order {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "状态：0未支付 1已支付 2已取消")
    private Integer status;
}
