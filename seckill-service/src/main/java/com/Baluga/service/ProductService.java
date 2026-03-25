package com.Baluga.service;

import com.Baluga.entity.Product;

import java.util.List;

public interface ProductService {

    void add(Product product);

    void update(Product product);

    void delete(Long id);

    List<Product> list();

    Product getById(Long id);
}
