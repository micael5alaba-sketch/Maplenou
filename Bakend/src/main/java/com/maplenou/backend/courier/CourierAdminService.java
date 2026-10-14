package com.maplenou.backend.courier;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.courier.dto.CreateDeliveryAgentRequest;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.UserRepository;
import com.maplenou.backend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Création directe d'un compte livreur par l'admin (cahier des charges §3.3/§3.4, "option B" :
 * livreur interne, dont les coordonnées sont transmises hors application par le porteur du
 * projet). Distinct de l'auto-inscription publique ({@code AuthService.register}) : ici c'est
 * l'admin qui choisit le mot de passe et le communique lui-même au livreur.
 */
@Service
@RequiredArgsConstructor
public class CourierAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserResponse createDeliveryAgent(CreateDeliveryAgentRequest request, User admin, String ip) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce numéro de téléphone est déjà utilisé");
        }

        User agent = User.builder()
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.DELIVERY_AGENT)
                .active(true)
                // Compte créé en interne par l'admin, identité déjà connue/confirmée hors app.
                .phoneVerified(true)
                .build();

        userRepository.save(agent);

        auditService.log(admin.getId(), AuditAction.DELIVERY_AGENT_CREATED,
                "USER", agent.getId(),
                "Compte livreur créé par l'admin : " + agent.getFullName() + " (" + agent.getPhoneNumber() + ")",
                ip);

        return UserResponse.from(agent);
    }
}
