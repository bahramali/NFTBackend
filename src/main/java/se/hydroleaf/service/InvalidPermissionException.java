package se.hydroleaf.service;

import java.util.List;

public class InvalidPermissionException extends RuntimeException {
    private final List<String> invalidPermissions;

    public InvalidPermissionException(List<String> invalidPermissions) {
        super("Invalid permissions provided");
        this.invalidPermissions = List.copyOf(invalidPermissions);
    }

    public List<String> invalidPermissions() {
        return invalidPermissions;
    }
}
