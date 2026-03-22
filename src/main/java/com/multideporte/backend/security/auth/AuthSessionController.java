package com.multideporte.backend.security.auth;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.security.user.AppUser;
import com.multideporte.backend.security.user.AppUserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthSessionController {

    private final AppUserRepository appUserRepository;
    private final AuthorizationCapabilityService authorizationCapabilityService;

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> getSession(Authentication authentication) {
        String username = authentication.getName();

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .sorted()
                .toList();

        AuthSessionResponse response = new AuthSessionResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                roles,
                authorizationCapabilityService.resolvePermissions(roles)
        );

        return ResponseEntity.ok(ApiResponse.success("AUTH_SESSION", "Sesion obtenida correctamente", response));
    }
}
