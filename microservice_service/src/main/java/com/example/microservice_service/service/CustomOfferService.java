package com.example.microservice_service.service;

import com.example.microservice_service.entity.CustomOffer;
import com.example.microservice_service.entity.enums.CustomOfferStatus;
import com.example.microservice_service.repository.CustomOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomOfferService {

    private final CustomOfferRepository customOfferRepository;
    private final FreelancerServiceService freelancerServiceService;

    public CustomOffer createOffer(CustomOffer offer, Long requesterId) {
        offer.setId(null);
        offer.setSenderId(requesterId);
        if (offer.getReceiverId() == null || offer.getReceiverId().equals(requesterId)) {
            throw new IllegalArgumentException("A different receiver is required");
        }
        // Optionally link to a service
        if (offer.getService() != null && offer.getService().getId() != null) {
            var service = freelancerServiceService.getServiceById(offer.getService().getId());
            if (!service.getShop().getFreelancerId().equals(requesterId)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
            }
            offer.setService(service);
        }
        offer.setStatus(CustomOfferStatus.PENDING);
        return customOfferRepository.save(offer);
    }

    public CustomOffer getOfferById(Long id) {
        return customOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom offer not found with id: " + id));
    }

    public List<CustomOffer> getOffersBySender(Long senderId) {
        return customOfferRepository.findBySenderId(senderId);
    }

    public List<CustomOffer> getOffersByReceiver(Long receiverId) {
        return customOfferRepository.findByReceiverId(receiverId);
    }

    public List<CustomOffer> getPendingOffersForReceiver(Long receiverId) {
        return customOfferRepository.findByReceiverIdAndStatus(receiverId, CustomOfferStatus.PENDING);
    }

    // Buyer accepts the offer — creates an order from it
    public CustomOffer acceptOffer(Long offerId, Long requesterId) {
        CustomOffer offer = getOfferById(offerId);
        requireReceiver(offer, requesterId);
        if (offer.getStatus() != CustomOfferStatus.PENDING) {
            throw new IllegalStateException("Only PENDING offers can be accepted.");
        }
        offer.setStatus(CustomOfferStatus.ACCEPTED);
        return customOfferRepository.save(offer);
    }

    public CustomOffer declineOffer(Long offerId, Long requesterId) {
        CustomOffer offer = getOfferById(offerId);
        requireReceiver(offer, requesterId);
        if (offer.getStatus() != CustomOfferStatus.PENDING) {
            throw new IllegalStateException("Only PENDING offers can be declined.");
        }
        offer.setStatus(CustomOfferStatus.DECLINED);
        return customOfferRepository.save(offer);
    }

    public CustomOffer expireOffer(Long offerId, String requesterRole) {
        requireAdmin(requesterRole);
        CustomOffer offer = getOfferById(offerId);
        offer.setStatus(CustomOfferStatus.EXPIRED);
        return customOfferRepository.save(offer);
    }

    public void deleteOffer(Long id, Long requesterId, String requesterRole) {
        CustomOffer offer = getOfferById(id);
        if (!offer.getSenderId().equals(requesterId) && !"ADMIN".equals(requesterRole)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
        customOfferRepository.deleteById(id);
    }
    public CustomOffer getOfferForParticipant(Long id, Long requesterId) {
        CustomOffer offer = getOfferById(id);
        if (!offer.getSenderId().equals(requesterId) && !offer.getReceiverId().equals(requesterId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
        return offer;
    }

    public void requireSender(Long senderId, Long requesterId) {
        if (!senderId.equals(requesterId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    public void requireReceiverId(Long receiverId, Long requesterId) {
        if (!receiverId.equals(requesterId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    private void requireReceiver(CustomOffer offer, Long requesterId) {
        requireReceiverId(offer.getReceiverId(), requesterId);
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }
}
