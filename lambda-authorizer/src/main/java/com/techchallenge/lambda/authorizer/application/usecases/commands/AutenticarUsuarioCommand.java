package com.techchallenge.lambda.authorizer.application.usecases.commands;

public class AutenticarUsuarioCommand {

    private final String username;
    private final String password;

    public AutenticarUsuarioCommand(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}