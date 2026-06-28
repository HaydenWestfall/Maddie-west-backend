package com.maddiewest.rentalservice.security;

import com.maddiewest.rentalservice.document.CoordinatorUser;
import com.maddiewest.rentalservice.repository.CoordinatorUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoordinatorUserDetailsService implements UserDetailsService {

    private final CoordinatorUserRepository coordinatorUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        CoordinatorUser user = coordinatorUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));

        return new User(user.getUsername(), user.getPasswordHash(),
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }
}
