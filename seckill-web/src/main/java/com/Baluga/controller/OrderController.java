package com.Baluga.controller;

import com.Baluga.Result;
import com.Baluga.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@Tag(name = "订单管理接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "创建订单")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true),
            @Parameter(name = "productId", description = "商品ID", required = true)
    })
    @PostMapping("/create")
    public Result create(Long userId, Long productId) {
        orderService.createOrder(userId, productId);
        return Result.success("下单成功");
    }

    @Operation(summary = "查询订单列表")
    @GetMapping("/list")
    public Result list() {
        return Result.success(orderService.list());
    }
}