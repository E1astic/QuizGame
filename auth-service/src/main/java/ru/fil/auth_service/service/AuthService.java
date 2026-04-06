package ru.fil.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.fil.auth_service.converter.UserConverter;
import ru.fil.auth_service.dto.UserLoginRequest;
import ru.fil.auth_service.dto.UserLoginResponse;
import ru.fil.auth_service.dto.UserRegisterRequest;
import ru.fil.auth_service.dto.UserRegisterResponse;
import ru.fil.auth_service.entity.User;
import ru.fil.auth_service.entity.UserDetailsImpl;
import ru.fil.auth_service.repository.UserRepository;
import ru.fil.auth_service.utils.JwtUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository repository;
    private final UserService userService;
    private final UserConverter converter;
    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;

    public UserRegisterResponse registerUser(UserRegisterRequest registerRequest) {
        User existingUser = repository.findByEmail(registerRequest.email());
        if (existingUser == null) {
            User user = converter.mapToUser(registerRequest);
            repository.save(user);
            log.info("User {} successfully registered", user.getEmail());
            return new UserRegisterResponse("Успешная регистрация");
        } else {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    public String authUserAndGetJwt(UserLoginRequest userLoginRequest) {
        try {
            Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(
                    userLoginRequest.email(), userLoginRequest.password()));
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return jwtUtils.generateJwt(userDetails);
        } catch (AuthenticationException e) {
            log.info("Authentication exception: {}", e.getMessage());
            throw e;
        }
    }
}
