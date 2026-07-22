package com.erichiroshi.blackfridaysaleflow.sale.domain.exception;

public class InvalidOrderStateTransitionException extends BusinessException {

    public InvalidOrderStateTransitionException(String message) {
        super(message);
    }
}
