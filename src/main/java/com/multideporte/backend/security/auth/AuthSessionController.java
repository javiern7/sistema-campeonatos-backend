package com.multideporte.backend.security.auth;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.security.user.AppUser;
import com.multideporte.backend.security.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthSessionController {

    private final AppUserRepository appUserRepository;
    private final AuthorizationCapabilityService authorizationCapabilityService;
    private final AuthTokenService authTokenService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthTokenResponse response = authTokenService.login(
                request.username(),
                request.password(),
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(ApiResponse.success("AUTH_LOGIN_SUCCESS", "Sesion iniciada correctamente", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(@Valid @RequestBody AuthRefreshRequest request) {
        AuthTokenResponse response = authTokenService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("AUTH_REFRESH_SUCCESS", "Sesion renovada correctamente", response));
    }

    @PostMapping("/logout")
    @PreAuthorize(SecurityPermissions.CAN_READ_AUTH_SESSION)
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authTokenService.logoutByAccessToken(resolveBearerToken(request));
        return ResponseEntity.ok(ApiResponse.success("AUTH_LOGOUT_SUCCESS", "Sesion cerrada correctamente"));
    }

    @GetMapping("/session")
    @PreAuthorize(SecurityPermissions.CAN_READ_AUTH_SESSION)
    public ResponseEntity<ApiResponse<AuthSessionResponse>> getSession(Authentication authentication) {
        String username = authentication.getName();

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .sorted()
                .toList();

        AuthSessionAuthenticationDetails authenticationDetails = resolveAuthenticationDetails(authentication);
        AuthSessionResponse response = new AuthSessionResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                authenticationDetails.authenticationScheme(),
                "STATELESS",
                authenticationDetails.sessionId(),
                authenticationDetails.accessTokenExpiresAt(),
                roles,
                authorizationCapabilityService.resolvePermissions(roles)
        );

        return ResponseEntity.ok(ApiResponse.success("AUTH_SESSION", "Sesion obtenida correctamente", response));
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(org.springframework.http.HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }
        return null;
    }

    private AuthSessionAuthenticationDetails resolveAuthenticationDetails(Authentication authentication) {
        if (authentication.getDetails() instanceof AuthSessionAuthenticationDetails authenticationDetails) {
            return authenticationDetails;
        }
        return new AuthSessionAuthenticationDetails("BEARER", null, null);
    }
}
