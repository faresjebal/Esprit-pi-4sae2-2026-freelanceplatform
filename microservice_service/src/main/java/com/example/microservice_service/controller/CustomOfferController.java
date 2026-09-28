package com.example.microservice_service.controller;

import com.example.microservice_service.entity.CustomOffer;
import com.example.microservice_service.service.CustomOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/custom-offers")
@RequiredArgsConstructor
public class CustomOfferController {

    private final CustomOfferService customOfferService;
    private final com.example.microservice_service.Security.JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<CustomOffer> createOffer(
            @RequestBody CustomOffer offer,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customOfferService.createOffer(offer, requesterId(authHeader)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomOffer> getOfferById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(customOfferService.getOfferForParticipant(id, requesterId(authHeader)));
    }

    @GetMapping("/sender/{senderId}")
    public ResponseEntity<List<CustomOffer>> getOffersBySender(
            @PathVariable Long senderId,
            @RequestHeader("Authorization") String authHeader) {
        customOfferService.requireSender(senderId, requesterId(authHeader));
        return ResponseEntity.ok(customOfferService.getOffersBySender(senderId));
    }

    @GetMapping("/receiver/{receiverId}")
    public ResponseEntity<List<CustomOffer>> getOffersByReceiver(
            @PathVariable Long receiverId,
            @RequestHeader("Authorization") String authHeader) {
        customOfferService.requireReceiverId(receiverId, requesterId(authHeader));
        return ResponseEntity.ok(customOfferService.getOffersByReceiver(receiverId));
    }

    @GetMapping("/receiver/{receiverId}/pending")
    public ResponseEntity<List<CustomOffer>> getPendingOffersForReceiver(
            @PathVariable Long receiverId,
            @RequestHeader("Authorization") String authHeader) {
        customOfferService.requireReceiverId(receiverId, requesterId(authHeader));
        return ResponseEntity.ok(customOfferService.getPendingOffersForReceiver(receiverId));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<CustomOffer> acceptOffer(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(customOfferService.acceptOffer(id, requesterId(authHeader)));
    }

    @PatchMapping("/{id}/decline")
    public ResponseEntity<CustomOffer> declineOffer(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(customOfferService.declineOffer(id, requesterId(authHeader)));
    }

    @PatchMapping("/{id}/expire")
    public ResponseEntity<CustomOffer> expireOffer(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(customOfferService.expireOffer(id, requesterRole(authHeader)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        customOfferService.deleteOffer(id, requesterId(authHeader), requesterRole(authHeader));
        return ResponseEntity.noContent().build();
    }
    private String bearerToken(String header) {
        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return header.substring(7);
    }

    private Long requesterId(String header) {
        try {
            Long id = jwtUtil.extractUserId(bearerToken(header));
            if (id == null) throw new IllegalArgumentException("Missing user ID");
            return id;
        } catch (RuntimeException ex) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private String requesterRole(String header) {
        try {
            String role = jwtUtil.extractRole(bearerToken(header));
            if (role == null) throw new IllegalArgumentException("Missing role");
            return role;
        } catch (RuntimeException ex) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
