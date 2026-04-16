package com.proops2026.userservice.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("user not found: " + userId);
    }
}
