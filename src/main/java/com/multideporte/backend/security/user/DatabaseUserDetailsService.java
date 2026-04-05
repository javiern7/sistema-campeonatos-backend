package com.multideporte.backend.security.user;

import com.multideporte.backend.security.auth.AuthorizationCapabilityService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final AuthorizationCapabilityService authorizationCapabilityService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UsernameNotFoundException("Usuario inactivo o bloqueado");
        }

        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                Stream.concat(
                                user.getRoles()
                                        .stream()
                                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode())),
                                authorizationCapabilityService.resolvePermissions(
                                                user.getRoles().stream().map(AppRole::getCode).toList()
                                        )
                                        .stream()
                                        .map(SimpleGrantedAuthority::new)
                        )
                        .collect(Collectors.toSet())
        );
    }
}
