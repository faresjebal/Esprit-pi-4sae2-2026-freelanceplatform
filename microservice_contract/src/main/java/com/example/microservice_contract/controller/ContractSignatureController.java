package com.example.microservice_contract.controller;

import com.example.microservice_contract.dto.ContractSignatureDto;
import com.example.microservice_contract.entity.ContractSignature;
import com.example.microservice_contract.Enum.SignatureStatus;
import com.example.microservice_contract.repository.IContractSignatureRepository;
import com.example.microservice_contract.service.IContractSignatureService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts/{contractId}/signatures")
@RequiredArgsConstructor
public class ContractSignatureController {

    private final IContractSignatureService    signatureService;
    private final IContractSignatureRepository signatureRepository;

    // ── Initiate (ADMIN only) ──────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<ContractSignatureDto.Response> initiateSignature(
            @PathVariable Long contractId,
            @Valid @RequestBody ContractSignatureDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(signatureService.initiateSignature(contractId, request));
    }

    // ── Auth status ────────────────────────────────────────────────────────────

    @GetMapping("/auth-status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(HttpServletRequest request) {
        String userEmail = (String) request.getAttribute("email");
        String userRole  = (String) request.getAttribute("role");
        Long   userId    = (Long)   request.getAttribute("userId");

        if (userEmail == null) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "message", "Not logged in"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "email",  userEmail,
                "role",   userRole,
                "userId", userId
        ));
    }

    // ── Can-sign check ─────────────────────────────────────────────────────────

    @GetMapping("/can-sign")
    public ResponseEntity<Map<String, Object>> canSign(
            @PathVariable Long contractId,
            @RequestParam String token,
            HttpServletRequest request) {

        String userEmail = (String) request.getAttribute("email");
        String userRole  = (String) request.getAttribute("role");
        Long   userId    = (Long)   request.getAttribute("userId");

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "authenticated", false,
                    "canSign",       false,
                    "message",       "Please log in to sign this contract",
                    "loginUrl",      "/login?redirect=/sign-contract/" + contractId + "?token=" + token
            ));
        }

        ContractSignature signature = signatureRepository.findByToken(token).orElse(null);

        if (signature == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "authenticated", true,
                    "canSign",       false,
                    "message",       "Invalid signing token"
            ));
        }

        if (signature.getStatus() == SignatureStatus.SIGNED) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "canSign",       false,
                    "alreadySigned", true,
                    "message",       "Contract already signed"
            ));
        }

        if (signature.getExpiresAt() != null &&
                LocalDateTime.now().isAfter(signature.getExpiresAt())) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "canSign",       false,
                    "expired",       true,
                    "message",       "Signing link has expired"
            ));
        }

        if (!signature.getSignerEmail().equalsIgnoreCase(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "authenticated", true,
                    "canSign",       false,
                    "wrongUser",     true,
                    "message",       "This signing link belongs to " + signature.getSignerEmail() +
                            ". You are logged in as " + userEmail,
                    "suggestion",    "Please log out and sign in with the correct account"
            ));
        }

        if (!signature.getSignerRole().equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "authenticated",  true,
                    "canSign",        false,
                    "roleMismatch",   true,
                    "expectedRole",   signature.getSignerRole(),
                    "yourRole",       userRole,
                    "message",        "Role mismatch. Expected: " + signature.getSignerRole()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "canSign",       true,
                "contractId",    contractId,
                "signerRole",    signature.getSignerRole(),
                "signerEmail",   signature.getSignerEmail(),
                "expiresAt",     signature.getExpiresAt(),
                "message",       "You are authorized to sign this contract"
        ));
    }

    // ── Submit signature (visual + crypto) ────────────────────────────────────

    /**
     * POST /api/contracts/{contractId}/signatures/sign
     *
     * Accepts both:
     *  - drawn canvas signatures: "data:image/png;base64,..."
     *  - uploaded image files converted to base64 on the frontend
     *
     * On success the response includes:
     *  - signatureData   – the stored image (base64, no prefix)
     *  - cryptoSignature – RSA-SHA256 signature of the canonical payload
     *  - cryptoPublicKey – public key for independent verification
     *  - keyFingerprint  – short hex fingerprint for audit logs
     *  - cryptoVerified  – server confirmed signature is valid
     */
    @PostMapping("/sign")
    public ResponseEntity<?> submitSignature(
            @PathVariable Long contractId,
            @Valid @RequestBody ContractSignatureDto.SignRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = (String) httpRequest.getAttribute("email");
        String userRole  = (String) httpRequest.getAttribute("role");
        Long   userId    = (Long)   httpRequest.getAttribute("userId");

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error",   "Authentication required",
                    "message", "Please log in to sign this contract"
            ));
        }

        try {
            ContractSignatureDto.Response signature = signatureService.submitSignature(
                    request, userId, userEmail, userRole);

            return ResponseEntity.ok(signature);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error",   "Authorization failed",
                    "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error",   "Cannot sign",
                    "message", e.getMessage()
            ));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error",   "Not found",
                    "message", e.getMessage()
            ));
        }
    }

    // ── Verify cryptographic signature ─────────────────────────────────────────

    /**
     * GET /api/contracts/{contractId}/signatures/{signatureId}/verify
     *
     * Re-runs RSA verification against the stored payload and public key.
     * Anyone with access to the record can call this — no auth required — to
     * independently confirm the signature has not been tampered with.
     */
    @GetMapping("/{signatureId}/verify")
    public ResponseEntity<ContractSignatureDto.VerifyResponse> verifyCryptoSignature(
            @PathVariable Long contractId,
            @PathVariable Long signatureId) {

        ContractSignatureDto.VerifyResponse result = signatureService.verifySignature(signatureId);
        HttpStatus status = result.isValid() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(result);
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('CLIENT', 'FREELANCER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<ContractSignatureDto.Response>> getSignatures(
            @PathVariable Long contractId) {
        return ResponseEntity.ok(signatureService.getSignaturesByContract(contractId));
    }

    @PreAuthorize("hasAnyAuthority('CLIENT', 'FREELANCER', 'ADMIN')")
    @GetMapping("/{signatureId}")
    public ResponseEntity<ContractSignatureDto.Response> getSignature(
            @PathVariable Long contractId,
            @PathVariable Long signatureId) {
        return ResponseEntity.ok(signatureService.getSignatureById(signatureId));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> isFullySigned(@PathVariable Long contractId) {
        return ResponseEntity.ok(Map.of(
                "fullySigned", signatureService.isFullySigned(contractId)));
    }
}