package com.example.microservice_service.service;

import com.example.microservice_service.entity.FreelancerService;
import com.example.microservice_service.entity.Shop;
import com.example.microservice_service.entity.enums.ServiceStatus;
import com.example.microservice_service.repository.FreelancerServiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FreelancerServiceService {

    private final FreelancerServiceRepository serviceRepository;
    private final ShopService shopService;

    @PersistenceContext
    private EntityManager entityManager;

    // Manual constructor for the final fields — Lombok can't mix with @PersistenceContext
    public FreelancerServiceService(FreelancerServiceRepository serviceRepository,
                                    ShopService shopService) {
        this.serviceRepository = serviceRepository;
        this.shopService = shopService;
    }

    @Transactional
    public FreelancerService createService(Long shopId, FreelancerService service, Long requesterId) {
        Shop shop = shopService.getShopById(shopId);
        if (shop == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Shop not found");
        }
        if (!shop.getFreelancerId().equals(requesterId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not your shop");
        }
        // getReference() creates a managed proxy — Hibernate correctly writes the FK
        Shop shopRef = entityManager.getReference(Shop.class, shopId);
        service.setShop(shopRef);
        service.setStatus(ServiceStatus.DRAFT);
        return serviceRepository.saveAndFlush(service);
    }

    public FreelancerService getServiceById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
    }

    public FreelancerService getServiceBySlug(String slug) {
        return serviceRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Service not found with slug: " + slug));
    }

    public List<FreelancerService> getServicesByShop(Long shopId) {
        return serviceRepository.findByShopId(shopId);
    }

    public List<FreelancerService> getActiveServices() {
        return serviceRepository.findByStatus(ServiceStatus.ACTIVE);
    }

    public List<FreelancerService> getActiveServicesByCategory(String category) {
        return serviceRepository.findByCategoryAndStatus(category, ServiceStatus.ACTIVE);
    }

    public List<FreelancerService> getPendingServices() {
        return serviceRepository.findByStatus(ServiceStatus.SUBMITTED);
    }

    @Transactional
    public FreelancerService updateService(Long id, FreelancerService updated, Long requesterId) {
        FreelancerService existing = getServiceById(id);
        requireOwner(existing, requesterId);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setTags(updated.getTags());
        existing.setPrice(updated.getPrice());
        existing.setDeliveryDays(updated.getDeliveryDays());
        existing.setRevisionCount(updated.getRevisionCount());
        existing.setRequirementsDescription(updated.getRequirementsDescription());
        existing.setMediaUrls(updated.getMediaUrls());
        return serviceRepository.save(existing);
    }

    @Transactional
    public FreelancerService submitForReview(Long id, Long requesterId) {
        FreelancerService service = getServiceById(id);
        requireOwner(service, requesterId);
        if (service.getStatus() != ServiceStatus.DRAFT && service.getStatus() != ServiceStatus.REJECTED) {
            throw new IllegalStateException("Only DRAFT or REJECTED services can be submitted for review.");
        }
        service.setStatus(ServiceStatus.SUBMITTED);
        return serviceRepository.save(service);
    }

    @Transactional
    public FreelancerService approveService(Long id, String requesterRole) {
        requireAdmin(requesterRole);
        FreelancerService service = getServiceById(id);
        service.setStatus(ServiceStatus.ACTIVE);
        service.setRejectionReason(null);
        return serviceRepository.save(service);
    }

    @Transactional
    public FreelancerService rejectService(Long id, String reason, String requesterRole) {
        requireAdmin(requesterRole);
        FreelancerService service = getServiceById(id);
        service.setStatus(ServiceStatus.REJECTED);
        service.setRejectionReason(reason);
        return serviceRepository.save(service);
    }

    @Transactional
    public FreelancerService togglePause(Long id, Long requesterId) {
        FreelancerService service = getServiceById(id);
        requireOwner(service, requesterId);
        if (service.getStatus() == ServiceStatus.ACTIVE) {
            service.setStatus(ServiceStatus.PAUSED);
        } else if (service.getStatus() == ServiceStatus.PAUSED) {
            service.setStatus(ServiceStatus.ACTIVE);
        } else {
            throw new IllegalStateException("Only ACTIVE or PAUSED services can be toggled.");
        }
        return serviceRepository.save(service);
    }

    @Transactional
    public FreelancerService archiveService(Long id, Long requesterId) {
        FreelancerService service = getServiceById(id);
        requireOwner(service, requesterId);
        service.setStatus(ServiceStatus.ARCHIVED);
        return serviceRepository.save(service);
    }

    @Transactional
    public void deleteService(Long id, Long requesterId) {
        requireOwner(getServiceById(id), requesterId);
        serviceRepository.deleteById(id);
    }
    private void requireOwner(FreelancerService service, Long requesterId) {
        if (requesterId == null || !service.getShop().getFreelancerId().equals(requesterId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not your service");
        }
    }

    private void requireAdmin(String requesterRole) {
        if (!"ADMIN".equals(requesterRole)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin required");
        }
    }
}
