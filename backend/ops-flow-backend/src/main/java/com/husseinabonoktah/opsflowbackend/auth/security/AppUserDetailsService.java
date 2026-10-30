package com.husseinabonoktah.opsflowbackend.auth.security;

import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
    @NonNull
    public UserDetails loadUserByUsername(
            @NonNull
            String username
    ) {

        User user =
                userRepository
                        .findByEmailWithAuthorities(username)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "User not found"
                                )
                        );

        return new AppUserPrincipal(user);
    }
}
