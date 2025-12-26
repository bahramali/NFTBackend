package se.hydroleaf.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserStatus;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString(exclude = "password")
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_app_user_email", columnNames = "email")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 128, nullable = false)
    private String email;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "order_confirmation_emails", nullable = false)
    @Builder.Default
    private boolean orderConfirmationEmails = true;

    @Column(name = "pickup_ready_notification", nullable = false)
    @Builder.Default
    private boolean pickupReadyNotification = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 32, nullable = false)
    private UserRole role;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "app_user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission", length = 64)
    @Builder.Default
    private Set<Permission> permissions = EnumSet.noneOf(Permission.class);

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false, columnDefinition = "varchar(32) default 'ACTIVE' not null")
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "invited", nullable = false, columnDefinition = "boolean default false not null")
    @Builder.Default
    private boolean invited = false;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "invite_token_hash", length = 128)
    private String inviteTokenHash;

    @Column(name = "invite_expires_at")
    private LocalDateTime inviteExpiresAt;

    @Column(name = "invite_used_at")
    private LocalDateTime inviteUsedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        normalizeEmail();
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        syncLifecycle();
    }

    @PreUpdate
    public void preUpdate() {
        normalizeEmail();
        syncLifecycle();
    }

    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    private void syncLifecycle() {
        if (status == null) {
            status = active ? UserStatus.ACTIVE : UserStatus.DISABLED;
        }
        if (status == UserStatus.INVITED) {
            active = false;
            invited = true;
        } else {
            invited = false;
            active = status == UserStatus.ACTIVE;
            inviteTokenHash = null;
            inviteExpiresAt = null;
        }
    }
}
