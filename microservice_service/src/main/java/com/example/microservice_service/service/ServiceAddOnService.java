package com.example.microservice_service.service;

import com.example.microservice_service.entity.FreelancerService;
import com.example.microservice_service.entity.ServiceAddOn;
import com.example.microservice_service.repository.ServiceAddOnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAddOnService {

    private final ServiceAddOnRepository addOnRepository;
    private final FreelancerServiceService freelancerServiceService;

    public ServiceAddOn createAddOn(Long serviceId, ServiceAddOn addOn, Long requesterId) {
        FreelancerService service = freelancerServiceService.getServiceById(serviceId);
        requireOwner(service, requesterId);
        addOn.setId(null);
        addOn.setService(service);
        return addOnRepository.save(addOn);
    }

    public ServiceAddOn getAddOnById(Long id) {
        return addOnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Add-on not found with id: " + id));
    }

    public List<ServiceAddOn> getAddOnsByService(Long serviceId) {
        return addOnRepository.findByServiceId(serviceId);
    }

    public ServiceAddOn updateAddOn(Long id, ServiceAddOn updated, Long requesterId) {
        ServiceAddOn existing = getAddOnById(id);
        requireOwner(existing.getService(), requesterId);
        existing.setTitle(updated.getTitle());
        existing.setPrice(updated.getPrice());
        existing.setExtraDeliveryDays(updated.getExtraDeliveryDays());
        return addOnRepository.save(existing);
    }

    public void deleteAddOn(Long id, Long requesterId) {
        requireOwner(getAddOnById(id).getService(), requesterId);
        addOnRepository.deleteById(id);
    }
    private void requireOwner(FreelancerService service, Long requesterId) {
        if (requesterId == null || !service.getShop().getFreelancerId().equals(requesterId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }
}
