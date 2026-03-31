package com.Baluga.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_product")
public class Product {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private Integer status;
}
