package com.multideporte.backend.security.user;

import com.multideporte.backend.common.exception.BusinessException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public Long requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new BusinessException("No se pudo resolver el usuario autenticado"));
    }

    public Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return Optional.of(user.getId());
        }

        return Optional.empty();
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
