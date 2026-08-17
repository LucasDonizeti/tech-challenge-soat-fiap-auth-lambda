package com.techchallenge.lambda.authorizer.application.usecases.responses;

public class AuthResponse {

    private final String token;
    private final String type;
    private final String username;
    private final String role;

    public AuthResponse(String token, String type, String username, String role) {
        this.token = token;
        this.type = type;
        this.username = username;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
