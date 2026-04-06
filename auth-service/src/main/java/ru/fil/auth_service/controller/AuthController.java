package ru.fil.auth_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.fil.auth_service.dto.UserLoginRequest;
import ru.fil.auth_service.dto.UserLoginResponse;
import ru.fil.auth_service.dto.UserRegisterRequest;
import ru.fil.auth_service.dto.UserRegisterResponse;
import ru.fil.auth_service.entity.UserDetailsImpl;
import ru.fil.auth_service.service.AuthService;

import java.security.Principal;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/register")
    public UserRegisterResponse register(@RequestBody UserRegisterRequest userRegisterRequest) {
        return authService.registerUser(userRegisterRequest);
    }

    @GetMapping("/login")
    public String getToken(@RequestBody UserLoginRequest userLoginRequest) {
        return authService.authUserAndGetJwt(userLoginRequest);
    }

    @GetMapping("/test")
    public String test(UserDetailsImpl user) {
        return user.getUsername();
    }
}
