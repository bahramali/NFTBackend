CREATE TABLE app_user (
    id bigint primary key,
    email varchar(128) not null,
    password varchar(255) not null,
    role varchar(32) not null,
    active boolean not null,
    status varchar(32) not null,
    invited boolean not null,
    created_at timestamp not null
);

INSERT INTO app_user (
    id,
    email,
    password,
    role,
    active,
    status,
    invited,
    created_at
) VALUES (
    1,
    'test@example.com',
    'secret',
    'ADMIN',
    true,
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP
);
