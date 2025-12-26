package se.hydroleaf.model;

import java.util.EnumSet;
import java.util.Set;

public enum AdminPreset {
    SUPER_ADMIN(EnumSet.allOf(Permission.class)),
    ADMIN_STANDARD(EnumSet.of(
            Permission.MONITORING_VIEW,
            Permission.STORE_VIEW,
            Permission.PRODUCTS_MANAGE,
            Permission.ORDERS_MANAGE,
            Permission.ADMIN_OVERVIEW_VIEW
    )),
    OPERATOR(EnumSet.of(
            Permission.MONITORING_VIEW,
            Permission.STORE_VIEW
    )),
    ADMIN_STORE_ONLY(EnumSet.of(
            Permission.STORE_VIEW,
            Permission.PRODUCTS_MANAGE,
            Permission.ORDERS_MANAGE
    )),
    ADMIN_MONITORING_ONLY(EnumSet.of(
            Permission.MONITORING_VIEW,
            Permission.MONITORING_CONTROL,
            Permission.MONITORING_CONFIG
    ));

    private final Set<Permission> permissions;

    AdminPreset(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> permissions() {
        return EnumSet.copyOf(permissions);
    }
}
