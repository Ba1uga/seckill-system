package com.Baluga.service;

import com.Baluga.entity.Order;

import java.util.List;

public interface OrderService {

    void createOrder(Long userId, Long productId);

    List<Order> list();
}
