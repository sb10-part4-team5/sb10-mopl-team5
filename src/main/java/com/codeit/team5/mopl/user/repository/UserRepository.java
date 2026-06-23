package com.codeit.team5.mopl.user.repository;

import com.codeit.team5.mopl.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
}
