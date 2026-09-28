package com.oasis.library.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.oasis.library.repository.UserRepository;

/** Loads users by email for Spring Security's login check. */
@Service
public class LibraryUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public LibraryUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmailIgnoreCase(email.trim())
                .map(LibraryUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
