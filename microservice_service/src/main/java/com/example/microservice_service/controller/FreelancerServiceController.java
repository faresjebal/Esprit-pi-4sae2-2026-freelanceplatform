package com.example.microservice_service.controller;

import com.example.microservice_service.entity.FreelancerService;
import com.example.microservice_service.service.FreelancerServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class FreelancerServiceController {

    private final FreelancerServiceService serviceService;
    private final com.example.microservice_service.Security.JwtUtil jwtUtil;

    @PostMapping("/shop/{shopId}")
    public ResponseEntity<FreelancerService> createService(
            @PathVariable Long shopId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {

        FreelancerService service = new FreelancerService();

        service.setTitle(((String) body.get("title")).trim());
        service.setDescription(((String) body.get("description")).trim());
        service.setCategory((String) body.get("category"));
        service.setTags(body.get("tags") != null ? (String) body.get("tags") : null);
        service.setMediaUrls(body.get("mediaUrls") != null ? (String) body.get("mediaUrls") : null);
        service.setRequirementsDescription(
                body.get("requirementsDescription") != null ? (String) body.get("requirementsDescription") : null
        );
        service.setRevisionCount(
                body.get("revisionCount") != null ? Integer.parseInt(body.get("revisionCount").toString()) : 0
        );
        service.setPrice(new BigDecimal(body.get("price").toString()));

        // Accept both field names — Angular may send either
        Object days = body.get("deliveryDays");
        if (days == null) days = body.get("deliveryTimeDays");
        if (days == null) throw new IllegalArgumentException("deliveryDays is required");
        service.setDeliveryDays(Integer.parseInt(days.toString()));

        // slug is auto-generated in @PrePersist, shop is set in createService()
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceService.createService(shopId, service, requesterId(authHeader)));
    }

    @GetMapping
    public ResponseEntity<List<FreelancerService>> getActiveServices() {
        return ResponseEntity.ok(serviceService.getActiveServices());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FreelancerService>> getPendingServices(
            @RequestHeader("Authorization") String authHeader) {
        requireAdmin(authHeader);
        return ResponseEntity.ok(serviceService.getPendingServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FreelancerService> getServiceById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        FreelancerService service = serviceService.getServiceById(id);
        requireVisible(service, authHeader);
        return ResponseEntity.ok(service);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<FreelancerService> getServiceBySlug(
            @PathVariable String slug,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        FreelancerService service = serviceService.getServiceBySlug(slug);
        requireVisible(service, authHeader);
        return ResponseEntity.ok(service);
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<FreelancerService>> getServicesByShop(
            @PathVariable Long shopId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        List<FreelancerService> services = serviceService.getServicesByShop(shopId);
        if (services.isEmpty()) return ResponseEntity.ok(services);
        boolean owner = authHeader != null && services.get(0).getShop().getFreelancerId().equals(requesterId(authHeader));
        boolean admin = authHeader != null && "ADMIN".equals(requesterRole(authHeader));
        return ResponseEntity.ok(owner || admin ? services : services.stream()
                .filter(s -> s.getStatus() == com.example.microservice_service.entity.enums.ServiceStatus.ACTIVE)
                .toList());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FreelancerService>> getServicesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(serviceService.getActiveServicesByCategory(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FreelancerService> updateService(
            @PathVariable Long id,
            @RequestBody FreelancerService service,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(serviceService.updateService(id, service, requesterId(authHeader)));
    }

    @PatchMapping("/{id}/submit")
    public ResponseEntity<FreelancerService> submitForReview(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(serviceService.submitForReview(id, requesterId(authHeader)));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<FreelancerService> approveService(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(serviceService.approveService(id, requesterRole(authHeader)));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<FreelancerService> rejectService(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(serviceService.rejectService(id, body.get("reason"), requesterRole(authHeader)));
    }

    @PatchMapping("/{id}/toggle-pause")
    public ResponseEntity<FreelancerService> togglePause(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(serviceService.togglePause(id, requesterId(authHeader)));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<FreelancerService> archiveService(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(serviceService.archiveService(id, requesterId(authHeader)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        serviceService.deleteService(id, requesterId(authHeader));
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
    private void requireAdmin(String header) {
        if (!"ADMIN".equals(requesterRole(header))) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireVisible(FreelancerService service, String header) {
        if (service.getStatus() == com.example.microservice_service.entity.enums.ServiceStatus.ACTIVE) return;
        if (header == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long id = requesterId(header);
        if (!service.getShop().getFreelancerId().equals(id) && !"ADMIN".equals(requesterRole(header))) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
