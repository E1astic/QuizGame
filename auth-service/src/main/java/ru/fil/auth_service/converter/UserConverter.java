package ru.fil.auth_service.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.fil.auth_service.dto.UserRegisterRequest;
import ru.fil.auth_service.entity.User;
import ru.fil.auth_service.entity.UserRole;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class UserConverter {

    private final PasswordEncoder passwordEncoder;

    public User mapToUser(UserRegisterRequest registerRequest) {
        return User.builder()
                .role(UserRole.PLAYER)
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .phone(registerRequest.phone())
                .name(registerRequest.name())
                .surname(registerRequest.surname())
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
