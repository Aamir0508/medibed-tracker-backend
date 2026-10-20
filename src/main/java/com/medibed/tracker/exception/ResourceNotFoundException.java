package com.medibed.tracker.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String what, Long id) {
        super(what + " with id " + id + " was not found");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
