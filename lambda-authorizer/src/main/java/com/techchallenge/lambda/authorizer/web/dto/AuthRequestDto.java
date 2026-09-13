package com.techchallenge.lambda.authorizer.web.dto;

import com.techchallenge.lambda.authorizer.application.usecases.commands.AutenticarUsuarioCommand;

public class AuthRequestDto {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AutenticarUsuarioCommand toCommand() {
        return new AutenticarUsuarioCommand(username, password);
    }
}
