package com.maddiewest.events.security;

import com.maddiewest.events.document.CoordinatorUser;
import com.maddiewest.events.repository.CoordinatorUserRepository;
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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        CoordinatorUser user = coordinatorUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + email));

        return new User(user.getEmail(), "",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }
}
