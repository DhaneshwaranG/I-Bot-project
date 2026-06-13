package com.ibot.service;

import com.ibot.dto.request.LoginRequest;
import com.ibot.dto.request.RegisterRequest;
import com.ibot.dto.response.JwtResponse;
import com.ibot.entity.User;
import com.ibot.exception.ResourceNotFoundException;
import com.ibot.repository.UserRepository;
import com.ibot.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtUtils jwtUtils;
        private final AuditService auditService;

        @Transactional
        public JwtResponse register(RegisterRequest request) {

                log.info("REGISTER STEP 1 - Checking email");

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new IllegalArgumentException(
                                        "Email is already registered: " + request.getEmail());
                }

                log.info("REGISTER STEP 2 - Building user");

                User user = User.builder()
                                .name(request.getName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .department(request.getDepartment())
                                .phone(request.getPhone())
                                .role("ROLE_USER")
                                .enabled(true)
                                .build();

                log.info("REGISTER STEP 3 - Saving user");

                user = userRepository.save(user);

                log.info("REGISTER STEP 4 - User saved. ID={}", user.getId());

                try {
                        auditService.log(
                                        "USER",
                                        user.getId(),
                                        "REGISTER",
                                        null,
                                        user.getEmail(),
                                        user.getId(),
                                        user.getName());

                        log.info("REGISTER STEP 5 - Audit logged");

                } catch (Exception e) {

                        log.error("AUDIT ERROR", e);

                }

                log.info("REGISTER STEP 6 - Authenticating user");

                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));

                log.info("REGISTER STEP 7 - Authentication successful");

                JwtResponse response = buildJwtResponse(user, authentication);

                log.info("REGISTER STEP 8 - JWT generated");

                return response;
        }

        public JwtResponse login(LoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));

                log.info("User logged in: {}", user.getEmail());
                auditService.log("USER", user.getId(), "LOGIN",
                                null, null, user.getId(), user.getName());

                return buildJwtResponse(user, authentication);
        }

        public JwtResponse refreshToken(String refreshToken) {
                if (!jwtUtils.validateToken(refreshToken)) {
                        throw new IllegalArgumentException("Invalid or expired refresh token");
                }
                String email = jwtUtils.getUsernameFromToken(refreshToken);
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                String newAccessToken = jwtUtils.generateTokenFromUsername(email);
                String newRefreshToken = jwtUtils.generateRefreshToken(email);

                return JwtResponse.builder()
                                .token(newAccessToken)
                                .refreshToken(newRefreshToken)
                                .type("Bearer")
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .role(user.getRole())
                                .expiresIn(jwtUtils.getJwtExpiration())
                                .build();
        }

        private JwtResponse buildJwtResponse(User user, Authentication authentication) {
                String accessToken = jwtUtils.generateToken(authentication);
                String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

                return JwtResponse.builder()
                                .token(accessToken)
                                .refreshToken(refreshToken)
                                .type("Bearer")
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .role(user.getRole())
                                .expiresIn(jwtUtils.getJwtExpiration())
                                .build();
        }

        public User getCurrentUser(String email) {
                return userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        }
}
