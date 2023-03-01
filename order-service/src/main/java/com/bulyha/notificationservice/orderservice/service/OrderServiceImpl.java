package com.bulyha.notificationservice.orderservice.service;

import com.bulyha.notificationservice.orderservice.dto.InventoryResponse;
import com.bulyha.notificationservice.orderservice.dto.OrderLineItemsDto;
import com.bulyha.notificationservice.orderservice.dto.OrderRequest;
import com.bulyha.notificationservice.orderservice.event.OrderPlacedEvent;
import com.bulyha.notificationservice.orderservice.model.Order;
import com.bulyha.notificationservice.orderservice.model.OrderLineItems;
import com.bulyha.notificationservice.orderservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;


    public OrderServiceImpl(OrderRepository orderRepository, WebClient.Builder webClientBuilder, Tracer tracer, KafkaTemplate kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClientBuilder;
        this.tracer = tracer;
        this.kafkaTemplate = kafkaTemplate;
    }

    public String placeOrder(OrderRequest orderRequest) {

        Order order = mapFromOrderRequestToOrder(orderRequest);

         List<String> skuCodes = order.getOrderLineItemsList().stream()
                 .map(OrderLineItems::getSkuCode)
                 .toList();

         log.info("Calling inventory service");

        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())) {
            // call inventory service, and place order if product is in stock
            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();
            boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                    .allMatch(InventoryResponse::getIsInStock);

            if(allProductsInStock && inventoryResponseArray.length != 0) {
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Oder placed successfully";
            }
            else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }
        }
        finally {
            inventoryServiceLookup.end();
        }



    }

    private Order mapFromOrderRequestToOrder(OrderRequest orderRequest) {

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapFromDto)
                .toList();

         return Order.builder()
                 .orderNumber(UUID.randomUUID().toString())
                 .orderLineItemsList(orderLineItems)
                 .build();
    }

    private OrderLineItems mapFromDto(OrderLineItemsDto orderLineItemsDto) {
        return OrderLineItems.builder()
                .price(orderLineItemsDto.getPrice())
                .quantity(orderLineItemsDto.getQuantity())
                .skuCode(orderLineItemsDto.getSkuCode())
                .build();

    }
}
