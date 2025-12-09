package se.hydroleaf.model;

public enum UserRole {
    SUPERUSER,
    ADMIN,
    CLIENT

    public boolean hasAllPermissions() {
        return this == SUPERUSER;
    }

    public boolean canAccessDashboard() {
        return this == SUPERUSER || this == ADMIN;
    }

    public boolean canAccessPersonalPage() {
        return this == SUPERUSER || this == ADMIN || this == CLIENT;
    }
}
