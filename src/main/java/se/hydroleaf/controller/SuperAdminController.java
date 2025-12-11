package se.hydroleaf.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.controller.dto.UserCreateRequest;
import se.hydroleaf.controller.dto.UserResponse;
import se.hydroleaf.controller.dto.UserUpdateRequest;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.UserService;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final AuthorizationService authorizationService;
    private final UserService userService;

    @GetMapping("/admins")
    public List<UserResponse> listAdmins(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requireSuperAdmin(user);
        return userService.listUsers().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createAdmin(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody AdminUpsertRequest request) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requireSuperAdmin(user);
        try {
            UserCreateRequest createRequest = new UserCreateRequest(
                    request.username(),
                    request.email(),
                    request.password(),
                    request.displayName(),
                    UserRole.ADMIN,
                    request.active(),
                    request.permissions()
            );
            return UserResponse.from(userService.create(createRequest));
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        }
    }

    @PutMapping("/admins/{id}")
    public UserResponse updateAdmin(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRequest request) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requireSuperAdmin(user);
        try {
            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    request.username(),
                    request.email(),
                    request.password(),
                    request.displayName(),
                    UserRole.ADMIN,
                    request.active(),
                    request.permissions()
            );
            return UserResponse.from(userService.update(id, updateRequest));
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        }
    }

    public record AdminUpsertRequest(String username, String email, String password, String displayName,
                                     Boolean active, Set<Permission> permissions) {
    }

    public record AdminUpdateRequest(String username, String email, String password, String displayName,
                                     Boolean active, Set<Permission> permissions) {
    }

}
