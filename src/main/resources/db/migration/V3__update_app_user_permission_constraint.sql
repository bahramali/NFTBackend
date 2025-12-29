ALTER TABLE app_user_permissions
    DROP CONSTRAINT IF EXISTS app_user_permissions_permission_check;

ALTER TABLE app_user_permissions
    ADD CONSTRAINT app_user_permissions_permission_check
    CHECK (permission IN (
        'MONITORING_VIEW',
        'MONITORING_CONTROL',
        'MONITORING_CONFIG',
        'STORE_VIEW',
        'CUSTOMERS_VIEW',
        'PRODUCTS_MANAGE',
        'ORDERS_MANAGE',
        'ADMIN_OVERVIEW_VIEW',
        'ADMIN_INVITE',
        'ADMIN_PERMISSIONS_MANAGE',
        'ADMIN_DISABLE',
        'AUDIT_VIEW'
    ));
