package com.Baluga.service.impl;

import com.Baluga.entity.Product;
import com.Baluga.mapper.ProductMapper;
import com.Baluga.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public void add(Product product) {
        productMapper.insert(product);
    }

    @Override
    public void update(Product product) {
        productMapper.updateById(product);
    }

    @Override
    public void delete(Long id) {
        productMapper.deleteById(id);
    }

    @Override
    public List<Product> list() {
        return productMapper.selectList(null);
    }

    @Override
    public Product getById(Long id) {
        return productMapper.selectById(id);
    }
}
