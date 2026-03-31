package com.Baluga.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_order")
public class Order {

    private Long id;

    private Long userId;

    private BigDecimal totalAmount;

    private Integer status;
}
