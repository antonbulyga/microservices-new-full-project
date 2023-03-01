package com.bulyha.notificationservice.orderservice.repository;

import com.bulyha.notificationservice.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

}
