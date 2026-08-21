package com.techchallenge.lambda.authorizer.domain.exceptions;

public class ValidacaoValorException extends DomainException {

    public ValidacaoValorException(String message) {
        super(message, "VALIDACAO_VALOR");
    }
}
