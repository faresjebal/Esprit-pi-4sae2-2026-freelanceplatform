package com.example.microservice_service.service;

import com.example.microservice_service.client.UserClient;
import com.example.microservice_service.dto.UserResponse;
import com.example.microservice_service.entity.FreelancerService;
import com.example.microservice_service.entity.Order;
import com.example.microservice_service.entity.enums.OrderStatus;
import com.example.microservice_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final FreelancerServiceService freelancerServiceService;
    private final UserClient userClient;

    public Order createOrder(Long buyerId, Long serviceId, String selectedAddOns, java.math.BigDecimal totalPrice) {
        // Validate buyer exists and is active (both CLIENT and FREELANCER can order)
        UserResponse buyer = userClient.getUserById(buyerId);
        if (!buyer.isVerified()) {
            throw new IllegalStateException("User must be verified to place an order.");
        }
        if (!buyer.isActive()) {
            throw new IllegalStateException("User account is disabled.");
        }

        FreelancerService service = freelancerServiceService.getServiceById(serviceId);

        // Cannot order your own service
        if (service.getShop().getFreelancerId().equals(buyerId)) {
            throw new IllegalStateException("You cannot order your own service.");
        }

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setSellerId(service.getShop().getFreelancerId());
        order.setService(service);
        order.setSelectedAddOns(selectedAddOns);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.PENDING_REQUIREMENTS);
        order.setRevisionsUsed(0);

        return orderRepository.save(order);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    public Order getOrderForParticipant(Long orderId, Long requesterId) {
        Order order = getOrderById(orderId);
        if (!order.getBuyerId().equals(requesterId) && !order.getSellerId().equals(requesterId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not a participant in this order");
        }
        return order;
    }

    public List<Order> getOrdersByServiceForSeller(Long serviceId, Long requesterId) {
        FreelancerService service = freelancerServiceService.getServiceById(serviceId);
        if (!service.getShop().getFreelancerId().equals(requesterId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not the seller of this service");
        }
        return orderRepository.findByServiceId(serviceId);
    }

    public List<Order> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    public List<Order> getOrdersBySeller(Long sellerId) {
        return orderRepository.findBySellerId(sellerId);
    }

    public List<Order> getOrdersByService(Long serviceId) {
        return orderRepository.findByServiceId(serviceId);
    }

    public Order submitRequirements(Long orderId, String requirementsAnswer, Long requesterId) {
        Order order = getOrderById(orderId);
        if (!order.getBuyerId().equals(requesterId)) {
            throw new IllegalStateException("Only the buyer can submit requirements.");
        }
        if (order.getStatus() != OrderStatus.PENDING_REQUIREMENTS) {
            throw new IllegalStateException("Requirements already submitted.");
        }
        order.setBuyerRequirementsAnswer(requirementsAnswer);
        order.setStatus(OrderStatus.IN_PROGRESS);
        int deliveryDays = order.getService().getDeliveryDays();
        order.setDeadline(LocalDateTime.now().plusDays(deliveryDays));
        return orderRepository.save(order);
    }

    public Order deliverOrder(Long orderId, String deliveryMessage, String deliveryFileUrls, Long requesterId) {
        Order order = getOrderById(orderId);
        if (!order.getSellerId().equals(requesterId)) {
            throw new IllegalStateException("Only the seller can deliver the order.");
        }
        if (order.getStatus() != OrderStatus.IN_PROGRESS && order.getStatus() != OrderStatus.REVISION_REQUESTED) {
            throw new IllegalStateException("Order is not in a deliverable state.");
        }
        order.setDeliveryMessage(deliveryMessage);
        order.setDeliveryFileUrls(deliveryFileUrls);
        order.setStatus(OrderStatus.DELIVERED);
        return orderRepository.save(order);
    }

    public Order requestRevision(Long orderId, String revisionNotes, Long requesterId) {
        Order order = getOrderById(orderId);
        if (!order.getBuyerId().equals(requesterId)) {
            throw new IllegalStateException("Only the buyer can request a revision.");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Can only request revision on a delivered order.");
        }
        if (order.getRevisionsUsed() >= order.getService().getRevisionCount()) {
            throw new IllegalStateException("Maximum revisions reached.");
        }
        order.setRevisionNotes(revisionNotes);
        order.setRevisionsUsed(order.getRevisionsUsed() + 1);
        order.setStatus(OrderStatus.REVISION_REQUESTED);
        return orderRepository.save(order);
    }

    public Order completeOrder(Long orderId, Long requesterId) {
        Order order = getOrderById(orderId);
        if (!order.getBuyerId().equals(requesterId)) {
            throw new IllegalStateException("Only the buyer can complete the order.");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Can only complete a delivered order.");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public Order cancelOrder(Long orderId, Long requesterId) {
        Order order = getOrderById(orderId);
        if (!order.getBuyerId().equals(requesterId) && !order.getSellerId().equals(requesterId)) {
            throw new IllegalStateException("Only buyer or seller can cancel the order.");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    public Order disputeOrder(Long orderId, Long requesterId) {
        Order order = getOrderById(orderId);
        if (!order.getBuyerId().equals(requesterId) && !order.getSellerId().equals(requesterId)) {
            throw new IllegalStateException("Only buyer or seller can dispute the order.");
        }
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot dispute a completed or cancelled order.");
        }
        order.setStatus(OrderStatus.DISPUTED);
        return orderRepository.save(order);
    }
}