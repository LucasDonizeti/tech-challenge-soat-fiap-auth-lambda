package com.techchallenge.lambda.authorizer.application.usecases.ports.input;

import com.techchallenge.lambda.authorizer.application.usecases.commands.AutenticarUsuarioCommand;
import com.techchallenge.lambda.authorizer.application.usecases.responses.AuthResponse;

public interface AutenticarUsuarioInput {
    AuthResponse execute(AutenticarUsuarioCommand command);
}
