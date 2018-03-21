package com.delphix.masking.initializer.exception;

public class InputException extends Exception {

    private String message;

    public InputException(String message) {
        this.message = message;
    }
}
