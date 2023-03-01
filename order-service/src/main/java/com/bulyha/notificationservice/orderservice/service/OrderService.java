package com.bulyha.notificationservice.orderservice.service;

import com.bulyha.notificationservice.orderservice.dto.OrderRequest;

public interface OrderService {
    String placeOrder(OrderRequest orderRequest);

}
