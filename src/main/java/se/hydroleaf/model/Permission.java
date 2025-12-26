package se.hydroleaf.model;

public enum Permission {
    MONITORING_VIEW("Monitoring", "View monitoring data"),
    MONITORING_CONTROL("Monitoring", "Control monitoring systems"),
    MONITORING_CONFIG("Monitoring", "Configure monitoring systems"),
    STORE_VIEW("Store", "View store data"),
    PRODUCTS_MANAGE("Store", "Manage products"),
    ORDERS_MANAGE("Store", "Manage orders"),
    CUSTOMERS_VIEW("Store", "View customers"),
    ADMIN_OVERVIEW_VIEW("Admin / Access", "View admin overview"),
    ADMIN_INVITE("Admin / Access", "Invite admins"),
    ADMIN_PERMISSIONS_MANAGE("Admin / Access", "Manage admin permissions"),
    ADMIN_DISABLE("Admin / Access", "Disable admins"),
    AUDIT_VIEW("Admin / Access", "View audit logs");

    private final String group;
    private final String label;

    Permission(String group, String label) {
        this.group = group;
        this.label = label;
    }

    public String group() {
        return group;
    }

    public String label() {
        return label;
    }
}
