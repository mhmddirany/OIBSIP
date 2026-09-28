package com.oasis.library.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.library.dto.Requests;
import com.oasis.library.dto.Responses;
import com.oasis.library.security.LibraryUserDetails;
import com.oasis.library.service.ApiException;
import com.oasis.library.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** Registration, JSON login and "who am I". Logout is handled by Spring Security (POST /api/auth/logout). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Responses.UserInfo> register(@Valid @RequestBody Requests.Register req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public Responses.UserInfo login(@Valid @RequestBody Requests.Login req,
                                    HttpServletRequest request, HttpServletResponse response) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(req.email().trim(), req.password()));
        } catch (DisabledException e) {
            throw ApiException.forbidden("Your account has been deactivated. Please contact the library");
        } catch (AuthenticationException e) {
            // Same message for unknown email or wrong password: don't reveal which one was wrong.
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Protect against session fixation, then store the login in the session.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        LibraryUserDetails principal = (LibraryUserDetails) auth.getPrincipal();
        return authService.me(principal.getId());
    }

    @GetMapping("/me")
    public Responses.UserInfo me(@AuthenticationPrincipal LibraryUserDetails me) {
        return authService.me(me.getId());
    }
}
