package com.multideporte.backend.security.user;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppPermissionRepository extends JpaRepository<AppPermission, Long> {

    List<AppPermission> findAllByOrderByCodeAsc();

    List<AppPermission> findByCodeIn(Collection<String> codes);
}
