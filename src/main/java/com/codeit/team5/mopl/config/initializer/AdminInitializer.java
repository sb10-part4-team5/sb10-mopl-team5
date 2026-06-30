package com.codeit.team5.mopl.config.initializer;

import com.codeit.team5.mopl.config.properties.AdminProperties;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.entity.UserRole;
import com.codeit.team5.mopl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findByEmail(adminProperties.email())
                .ifPresentOrElse(existingAdmin -> {
                    if (existingAdmin.getRole() == UserRole.ADMIN) {
                        log.info("Admin init skip: 이미 ADMIN 계정이 존재합니다.");
                        return;
                    }

                    existingAdmin.updateRole(UserRole.ADMIN);
                    userRepository.save(existingAdmin);
                    log.info("Admin init recover: 기존 admin 계정의 권한을 ADMIN으로 복구했습니다.");
                }, () -> {
                    try {
                        User admin = User.create(
                                adminProperties.email(),
                                passwordEncoder.encode(adminProperties.password()),
                                adminProperties.name()
                        );

                        admin.updateRole(UserRole.ADMIN);
                        userRepository.save(admin);

                        log.info("Admin init success: ADMIN 계정이 초기화되었습니다.");
                    } catch (DataIntegrityViolationException e) {
                        userRepository.findByEmail(adminProperties.email())
                                .ifPresent(existingAdmin -> {
                                    if (existingAdmin.getRole() != UserRole.ADMIN) {
                                        existingAdmin.updateRole(UserRole.ADMIN);
                                        userRepository.save(existingAdmin);
                                    }
                                });

                        log.info("Admin init recovered after duplicate insert race.");
                    }
                });
    }
}
