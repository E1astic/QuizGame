package ru.fil.auth_service.dto;

public record UserRegisterRequest(
        String email, String password, String phone, String name, String surname) {
}
