package com.Baluga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "秒杀请求参数")
public class SeckillDTO {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "秒杀商品ID", example = "1001")
    private Long seckillProductId;
}
