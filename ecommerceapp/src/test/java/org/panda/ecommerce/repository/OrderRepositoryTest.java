package org.panda.ecommerce.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.panda.ecommerce.entity.Order;
import org.panda.ecommerce.entity.Role;
import org.panda.ecommerce.entity.User;
import org.panda.ecommerce.enums.OrderStatus;
import org.panda.ecommerce.enums.RoleName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role role = roleRepository.save(new Role(null, RoleName.ROLE_USER));
        testUser = userRepository.save(User.builder()
                .name("Test").email("test@example.com")
                .password("encoded").roles(Set.of(role)).build());
    }

    @Test
    void findByUserId_shouldReturnUserOrders() {
        orderRepository.save(Order.builder().user(testUser)
                .totalAmount(new BigDecimal("49.99")).shippingAddress("123 St").build());
        orderRepository.save(Order.builder().user(testUser)
                .totalAmount(new BigDecimal("99.99")).shippingAddress("456 Ave").build());

        Page<Order> orders = orderRepository.findByUserId(testUser.getId(), PageRequest.of(0, 10));

        assertThat(orders.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByStatus_shouldReturnMatchingOrders() {
        orderRepository.save(Order.builder().user(testUser)
                .status(OrderStatus.PENDING).totalAmount(new BigDecimal("10.00"))
                .shippingAddress("123 St").build());
        orderRepository.save(Order.builder().user(testUser)
                .status(OrderStatus.CONFIRMED).totalAmount(new BigDecimal("20.00"))
                .shippingAddress("456 Ave").build());

        Page<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING, PageRequest.of(0, 10));

        assertThat(pending.getTotalElements()).isEqualTo(1);
        assertThat(pending.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findByIdWithItems_shouldReturnOrderWithItems() {
        Order order = orderRepository.save(Order.builder().user(testUser)
                .totalAmount(new BigDecimal("29.99")).shippingAddress("789 Blvd").build());

        Optional<Order> found = orderRepository.findByIdWithItems(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(order.getId());
    }
}
