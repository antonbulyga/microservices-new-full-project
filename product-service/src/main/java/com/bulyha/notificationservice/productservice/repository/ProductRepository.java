package com.bulyha.notificationservice.productservice.repository;

import com.bulyha.notificationservice.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
