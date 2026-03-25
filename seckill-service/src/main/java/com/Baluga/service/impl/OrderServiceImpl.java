package com.Baluga.service.impl;

import com.Baluga.entity.Order;
import com.Baluga.entity.Product;
import com.Baluga.mapper.OrderMapper;
import com.Baluga.mapper.ProductMapper;
import com.Baluga.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    @Transactional
    public void createOrder(Long userId, Long productId) {

        // 1. 查询商品
        Product product = productMapper.selectById(productId);

        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        if (product.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }

        if (product.getStock() <= 0) {
            throw new RuntimeException("库存不足");
        }

        // 2. 扣库存（阶段1：简单扣减）
        product.setStock(product.getStock() - 1);
        productMapper.updateById(product);

        // 3. 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(product.getPrice());
        order.setStatus(0);

        orderMapper.insert(order);
    }

    @Override
    public List<Order> list() {
        return orderMapper.selectList(null);
    }
}
