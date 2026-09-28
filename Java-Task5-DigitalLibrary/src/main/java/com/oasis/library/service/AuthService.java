package com.oasis.library.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oasis.library.dto.Requests;
import com.oasis.library.dto.Responses;
import com.oasis.library.model.Role;
import com.oasis.library.model.User;
import com.oasis.library.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final DtoMapper mapper;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, DtoMapper mapper) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    /** Public registration always creates a normal member — never an admin. */
    @Transactional
    public Responses.UserInfo register(Requests.Register req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = new User(req.fullName().trim(), email, passwordEncoder.encode(req.password()), Role.USER);
        return mapper.user(users.save(user));
    }

    @Transactional(readOnly = true)
    public Responses.UserInfo me(Long userId) {
        return users.findById(userId).map(mapper::user)
                .orElseThrow(() -> ApiException.notFound("User"));
    }
}
