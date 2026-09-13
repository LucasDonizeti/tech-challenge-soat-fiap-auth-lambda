package com.techchallenge.lambda.authorizer.domain.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DomainException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(DomainException.class);

    private final String errorCode;

    protected DomainException(String message) {
        this(message, null);
    }

    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : this.getClass().getSimpleName();
        logger.warn("DomainException lançada - errorCode: {}, message: {}", errorCode, message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}

