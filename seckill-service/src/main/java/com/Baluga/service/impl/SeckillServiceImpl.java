package com.Baluga.service.impl;

import com.Baluga.Result;
import com.Baluga.dto.SeckillDTO;
import com.Baluga.entity.Order;
import com.Baluga.entity.SeckillOrder;
import com.Baluga.entity.SeckillProduct;
import com.Baluga.mapper.OrderMapper;
import com.Baluga.mapper.SeckillOrderMapper;
import com.Baluga.mapper.SeckillProductMapper;
import com.Baluga.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillProductMapper seckillProductMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Override
    @Transactional
    public Result doSeckill(SeckillDTO dto) {
        Long userId = dto.getUserId();
        Long seckillProductId = dto.getSeckillProductId();

        // 1. 查秒杀商品
        SeckillProduct sp = seckillProductMapper.selectById(seckillProductId);

        if (sp.getStock() <= 0) {
            return Result.error("库存不足");
        }

        // 2. 扣库存（简单版，后面优化）
        sp.setStock(sp.getStock() - 1);
        seckillProductMapper.updateById(sp);

        // 3. 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(sp.getSeckillPrice());
        orderMapper.insert(order);

        // 4. 创建秒杀订单（防重复）
        SeckillOrder so = new SeckillOrder();
        so.setUserId(userId);
        so.setSeckillProductId(seckillProductId);
        so.setOrderId(order.getId());

        seckillOrderMapper.insert(so);

        return Result.success("秒杀成功");
    }
}