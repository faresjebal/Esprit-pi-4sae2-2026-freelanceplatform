package com.example.microservice_service.controller;

import com.example.microservice_service.entity.Order;
import com.example.microservice_service.Security.JwtUtil;
import com.example.microservice_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long buyerId = jwtUtil.extractUserId(token);
        Long serviceId = Long.valueOf(body.get("serviceId").toString());
        String selectedAddOns = (String) body.getOrDefault("selectedAddOns", null);
        BigDecimal totalPrice = new BigDecimal(body.get("totalPrice").toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(buyerId, serviceId, selectedAddOns, totalPrice));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader.substring(7));
        return ResponseEntity.ok(orderService.getOrderForParticipant(id, userId));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(orderService.getOrdersByBuyer(userId));
    }

    @GetMapping("/my-sales")
    public ResponseEntity<List<Order>> getMySales(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(orderService.getOrdersBySeller(userId));
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<Order>> getOrdersByService(
            @PathVariable Long serviceId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.extractUserId(authHeader.substring(7));
        return ResponseEntity.ok(orderService.getOrdersByServiceForSeller(serviceId, userId));
    }

    @PatchMapping("/{id}/requirements")
    public ResponseEntity<Order> submitRequirements(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(orderService.submitRequirements(id, body.get("requirementsAnswer"), userId));
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<Order> deliverOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(orderService.deliverOrder(id, body.get("deliveryMessage"), body.get("deliveryFileUrls"), userId));
    }

    @PatchMapping("/{id}/revision")
    public ResponseEntity<Order> requestRevision(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(orderService.requestRevision(id, body.get("revisionNotes"), userId));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Order> completeOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(orderService.completeOrder(id, userId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(orderService.cancelOrder(id, userId));
    }

    @PatchMapping("/{id}/dispute")
    public ResponseEntity<Order> disputeOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(orderService.disputeOrder(id, userId));
    }
}