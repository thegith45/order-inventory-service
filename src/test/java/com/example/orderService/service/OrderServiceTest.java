package com.example.orderService.service;

import com.example.orderService.dto.OrderItemRequest;
import com.example.orderService.dto.OrderResponse;
import com.example.orderService.dto.PlaceOrderRequest;
import com.example.orderService.entity.*;
import com.example.orderService.repository.OrderRepository;
import com.example.orderService.repository.ProductRepository;
import com.example.orderService.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private Product product;

    @BeforeEach
    void setUp() throws Exception {
        customer = new User(
                "Test Customer",
                "customer@example.com",
                "hashedPassword",
                UserRole.CUSTOMER
        );

        product = new Product(
                "Keyboard",
                "Mechanical Keyboard",
                new BigDecimal("2500.00"),
                10
        );
        setProductId(product, 1L);
    }

    private void setUserId(User user, Long id) throws Exception {
        var field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
    private void setProductId(Product product, Long id) throws Exception {
        var field = Product.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(product, id);
    }

    @Test
    void placeOrderSuccessfully() {

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(customer));

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, 2))
        );

        OrderResponse response = orderService.placeOrder(
                "customer@example.com",
                request
        );

        assertEquals(new BigDecimal("5000.00"), response.totalAmount());
        assertEquals(8, product.getStock());
        assertEquals(OrderStatus.PLACED, response.status());
        assertEquals(1, response.items().size());

        verify(orderRepository).save(any(Order.class));
        verify(productRepository).findByIdForUpdate(1L);
    }

    @Test
    void placeOrderFailsWhenStockIsInsufficient() {

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(customer));

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(1L, 12))
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.placeOrder("customer@example.com", request)
        );

        assertEquals(409, exception.getStatusCode().value());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrderSuccessfully() throws Exception {
        setProductId(product, 1L);

        Order order = new Order(customer, new BigDecimal("5000.00"));

        OrderItem item = new OrderItem(product, 2, new BigDecimal("2500.00"));
        order.addItem(item);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(customer));

        setUserId(customer, 1L); //not by MySQL

        when(orderRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        OrderResponse response = orderService.cancelOrder(
                1L,
                "customer@example.com",
                false
        );

        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(12, product.getStock());

        verify(productRepository).findByIdForUpdate(1L);
    }

    @Test
    void cancelOrderFailsWhenAlreadyCancelled() throws Exception {

        setUserId(customer, 1L);

        Order order = new Order(customer, new BigDecimal("5000.00"));

        // First cancellation has already happened.
        order.setStatus(OrderStatus.CANCELLED);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(customer));

        when(orderRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.cancelOrder(
                        1L,
                        "customer@example.com",
                        false
                )
        );

        assertEquals(409, exception.getStatusCode().value());

        // No product stock should be restored again.
        verify(productRepository, never()).findByIdForUpdate(anyLong());
    }
}