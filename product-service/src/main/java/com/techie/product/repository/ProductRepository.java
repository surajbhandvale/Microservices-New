package com.techie.product.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.techie.product.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
}
