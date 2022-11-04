package com.techie.order.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.techie.order.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
