package com.Baluga.controller;

import com.Baluga.Result;
import com.Baluga.dto.SeckillDTO;
import com.Baluga.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill")
@Tag(name = "秒杀模块")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @PostMapping("/doSeckill")
    @Operation(summary = "执行秒杀")
    public Result doSeckill(@RequestBody SeckillDTO dto) {
        return seckillService.doSeckill(dto);
    }
}