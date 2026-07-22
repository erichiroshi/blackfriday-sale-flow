package com.erichiroshi.blackfridaysaleflow.sale.domain.exception;

/**
 * Base type for all business rule violations. Kept in the domain layer so
 * that use cases can throw it without any dependency on Spring's exception
 * hierarchy.
 */
public abstract class BusinessException extends RuntimeException {
    protected BusinessException(String message) {
        super(message);
    }
}
