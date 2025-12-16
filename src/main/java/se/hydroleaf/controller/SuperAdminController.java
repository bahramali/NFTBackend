package se.hydroleaf.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.controller.dto.AdminInviteRequest;
import se.hydroleaf.controller.dto.AdminPermissionsUpdateRequest;
import se.hydroleaf.controller.dto.AdminResponse;
import se.hydroleaf.controller.dto.AdminStatusUpdateRequest;
import se.hydroleaf.controller.dto.ResendInviteRequest;
import se.hydroleaf.service.AdminLifecycleService;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final AuthorizationService authorizationService;
    private final AdminLifecycleService adminLifecycleService;

    private void requireSuperAdmin(String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requireSuperAdmin(user);
    }

    @GetMapping("/admins")
    public List<AdminResponse> listAdmins(@RequestHeader(name = "Authorization", required = false) String token) {
        requireSuperAdmin(token);
        return adminLifecycleService.listAdmins().stream()
                .map(AdminResponse::from)
                .toList();
    }

    @PostMapping("/admins/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminResponse inviteAdmin(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody AdminInviteRequest request) {
        requireSuperAdmin(token);
        AdminLifecycleService.InviteResult result = adminLifecycleService.inviteAdmin(
                request.email(), request.displayName(), request.permissions(), request.expiresInHours(), request.expiresAt());
        return AdminResponse.from(result.user());
    }

    @PutMapping("/admins/{id}/permissions")
    public AdminResponse updatePermissions(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody AdminPermissionsUpdateRequest request) {
        requireSuperAdmin(token);
        return AdminResponse.from(adminLifecycleService.updatePermissions(id, request.permissions()));
    }

    @PutMapping("/admins/{id}/status")
    public AdminResponse updateStatus(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusUpdateRequest request) {
        requireSuperAdmin(token);
        return AdminResponse.from(adminLifecycleService.updateStatus(id, request.status(), request.active()));
    }

    @DeleteMapping("/admins/{idOrEmail}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAdmin(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable String idOrEmail) {
        requireSuperAdmin(token);
        adminLifecycleService.deleteAdmin(idOrEmail);
    }

    @PostMapping("/admins/{id}/resend-invite")
    public AdminResponse resendInvite(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ResendInviteRequest request) {
        requireSuperAdmin(token);
        Integer expiry = request != null ? request.expiresInHours() : null;
        AdminLifecycleService.InviteResult result = adminLifecycleService.resendInvite(id, expiry);
        return AdminResponse.from(result.user());
    }
}
