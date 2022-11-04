package com.techie.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.techie.order.dto.InventoryResponse;
import com.techie.order.dto.OrderLineItemsDto;
import com.techie.order.dto.OrderRequest;
import com.techie.order.model.Order;
import com.techie.order.model.OrderLineItems;
import com.techie.order.repository.OrderRepository;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

	private final OrderRepository orderRepository;
	private final WebClient.Builder webClientBuilder;
	private final Tracer tracer;

	public String placeOrder(OrderRequest orderRequest) {
		Order order = new Order();
		order.setOrderNumber(UUID.randomUUID().toString());

		List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToDto)
				.collect(Collectors.toList());

		order.setOrderLineItemsList(orderLineItems);

		List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode)
				.collect(Collectors.toList());
        log.info("*****sku code in order serveice ",skuCodes.toString());

		/*InventoryResponse[] inventoryResponsArray = webClientBuilder.build().get()
				.uri("http://localhost:8082/api/inventory",
						uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
				.retrieve()
				.bodyToMono(InventoryResponse[].class)
				.block();

		boolean allProductsInStock = Arrays.stream(inventoryResponsArray)
				.allMatch(InventoryResponse::isInStock);*/
		Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

		try (Tracer.SpanInScope isLookup = tracer.withSpan(inventoryServiceLookup.start())){
			inventoryServiceLookup.tag("call", "inventory-service");
		}finally {
			inventoryServiceLookup.end();
		}

		InventoryResponse[] inventoryResponsArray = this.webClientBuilder.build()
				.get()
				.uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder
				//.scheme("http")
				//.host("localhost")
				//.port(8082)
				//.path("/api/inventory")
				.queryParam("skuCode", skuCodes)
				.build()
				)
				.retrieve()
				.bodyToMono(InventoryResponse[].class)
				.block();

		boolean allProductsInStock = Arrays.stream(inventoryResponsArray).allMatch(InventoryResponse::isInStock);

		if(inventoryResponsArray.length == 0)
			allProductsInStock = false;

		if (allProductsInStock) {
			orderRepository.save(order);
		} else {
			throw new IllegalArgumentException("Product is not in stock, please try again later.");
		}

		return "Order Placed Successfully";

	}

	private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
		OrderLineItems orderLineItems = new OrderLineItems();
		orderLineItems.setPrice(orderLineItemsDto.getPrice());
		orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
		orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
		return orderLineItems;
	}
}
