package com.codeit.team5.mopl.auth.security.details;

import com.codeit.team5.mopl.auth.exception.InvalidCredentialsException;
import com.codeit.team5.mopl.auth.mapper.AuthUserMapper;
import com.codeit.team5.mopl.auth.support.EmailNormalizer;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoplUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthUserMapper authUserMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws InvalidCredentialsException {
        String normalizeEmail = EmailNormalizer.normalize(email);
        User user = userRepository.findByEmail(normalizeEmail)
                .orElseThrow(() -> new InvalidCredentialsException(normalizeEmail));

        return new MoplUserDetails(authUserMapper.toAuthUser(user), user.getPassword());
    }
}
