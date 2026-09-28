package com.example.microservice_service.controller;

import com.example.microservice_service.entity.ServiceAddOn;
import com.example.microservice_service.service.ServiceAddOnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/addons")
@RequiredArgsConstructor
public class ServiceAddOnController {

    private final ServiceAddOnService addOnService;
    private final com.example.microservice_service.Security.JwtUtil jwtUtil;

    @PostMapping("/service/{serviceId}")
    public ResponseEntity<ServiceAddOn> createAddOn(
            @PathVariable Long serviceId,
            @RequestBody ServiceAddOn addOn,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addOnService.createAddOn(serviceId, addOn, requesterId(authHeader)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceAddOn> getAddOnById(@PathVariable Long id) {
        return ResponseEntity.ok(addOnService.getAddOnById(id));
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<ServiceAddOn>> getAddOnsByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(addOnService.getAddOnsByService(serviceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceAddOn> updateAddOn(
            @PathVariable Long id,
            @RequestBody ServiceAddOn addOn,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(addOnService.updateAddOn(id, addOn, requesterId(authHeader)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddOn(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        addOnService.deleteAddOn(id, requesterId(authHeader));
        return ResponseEntity.noContent().build();
    }
    private Long requesterId(String header) {
        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            Long id = jwtUtil.extractUserId(header.substring(7));
            if (id == null) throw new IllegalArgumentException("Missing user ID");
            return id;
        } catch (RuntimeException ex) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
