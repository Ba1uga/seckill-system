package com.Baluga.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_seckill_product")
@Schema(name = "SeckillProduct", description = "秒杀商品")
public class SeckillProduct {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "秒杀价格")
    private BigDecimal seckillPrice;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "版本号")
    private Integer version;
}