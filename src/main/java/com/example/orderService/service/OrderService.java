package com.example.orderService.service;

import com.example.orderService.dto.OrderItemRequest;
import com.example.orderService.dto.OrderResponse;
import com.example.orderService.dto.PlaceOrderRequest;
import com.example.orderService.entity.Order;
import com.example.orderService.entity.OrderItem;
import com.example.orderService.entity.Product;
import com.example.orderService.entity.User;
import com.example.orderService.repository.OrderRepository;
import com.example.orderService.repository.ProductRepository;
import com.example.orderService.repository.UserRepository;
import com.example.orderService.entity.OrderStatus;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse placeOrder(
            String customerEmail, PlaceOrderRequest request) {

        // 1. Find the customer using their authenticated email
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Customer not found"
                ));

        // 2. Combine duplicate product IDs and sort IDs.
        Map<Long, Integer> quantities = new TreeMap<>();

        for (OrderItemRequest item : request.items()) {
            try {
                quantities.merge(
                        item.productId(),
                        item.quantity(),
                        Math::addExact
                );
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Total quantity is too large"
                );
            }
        }

        // 3. Create the order
        Order order = new Order(customer, BigDecimal.ZERO);

        BigDecimal totalAmt = BigDecimal.ZERO;

        // 4. Lock each product, validate stock and calculate total
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            // this is synchronized process
            Product product = productRepository
                    .findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Product not found: " + productId
                    ));

            if (product.getStock() < quantity) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for the product: " + product.getName()
                );
            }

            // 5. Reduce inventory stock
            product.setStock(product.getStock() - quantity);

            // 6. Calculate price using the current db price
            BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            totalAmt = totalAmt.add(total);

            // 7. Storing the price at the time of order
            OrderItem orderItem = new OrderItem(
                    product,
                    quantity,
                    product.getPrice()
            );
            order.addItem(orderItem);
        }

        // 8. Set final order amount
        order.setTotalAmount(totalAmt);
        // 9. Save order, through cascade.
        Order savedOrder = orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(
            String customerEmail, boolean isAdmin, Pageable pageable){
        Page<Order> orders;
        if(isAdmin){
            //admin can see all cust. orders
            orders = orderRepository.findAll(pageable);
        }
        else{
            //customer can see only their
            User customer = userRepository.findByEmail(customerEmail)
                    .orElseThrow(()->new ResponseStatusException(
                            HttpStatus.NOT_FOUND,"Customer not found"));
            orders = orderRepository.findByUserId(customer.getId(),pageable);
        }
        return orders.map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId
            ,String userEmail, boolean isAdmin){
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(()->new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "user not found"
                ));

        Order order;
        if(isAdmin){
            order = orderRepository.findById(orderId)
                    .orElseThrow(()->new ResponseStatusException(
                            HttpStatus.NOT_FOUND,"order not found"));
        }
        else{
            order = orderRepository.findByIdAndUserId(orderId, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Order not found"));
        }

        if(order.getStatus() != OrderStatus.PLACED){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,"only placed order can be cancelled"
            );
        }
        for(OrderItem item:order.getItems()){
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Product not found"));
            product.setStock(product.getStock()+ item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);

        return OrderResponse.from(order);
    }
}