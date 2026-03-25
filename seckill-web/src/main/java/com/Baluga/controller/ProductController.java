package com.Baluga.controller;

import com.Baluga.Result;
import com.Baluga.entity.Product;
import com.Baluga.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
@Tag(name = "商品管理接口")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Operation(summary = "新增商品")
    @PostMapping("/add")
    public Result add(@RequestBody Product product) {
        productService.add(product);
        return Result.success("添加成功");
    }

    @Operation(summary = "修改商品")
    @PostMapping("/update")
    public Result update(@RequestBody Product product) {
        productService.update(product);
        return Result.success("修改成功");
    }

    @Operation(summary = "删除商品")
    @Parameter(name = "id", description = "商品ID", required = true)
    @GetMapping("/delete")
    public Result delete(Long id) {
        productService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "查询商品列表")
    @GetMapping("/list")
    public Result list() {
        return Result.success(productService.list());
    }

    @Operation(summary = "查询商品详情")
    @Parameter(name = "id", description = "商品ID", required = true)
    @GetMapping("/get")
    public Result get(Long id) {
        return Result.success(productService.getById(id));
    }
}