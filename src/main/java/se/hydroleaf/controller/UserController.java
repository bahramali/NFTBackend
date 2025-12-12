package se.hydroleaf.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.controller.dto.UserCreateRequest;
import se.hydroleaf.controller.dto.UserResponse;
import se.hydroleaf.controller.dto.UserUpdateRequest;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthorizationService authorizationService;

    private AuthenticatedUser requireManageUsers(String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.MANAGE_USERS);
        return user;
    }

    @GetMapping
    public List<UserResponse> listUsers(@RequestHeader(name = "Authorization", required = false) String token) {
        requireManageUsers(token);
        return userService.listUsers().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UserResponse getById(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id) {
        requireManageUsers(token);
        return UserResponse.from(userService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody UserCreateRequest request) {
        requireManageUsers(token);
        User user = userService.create(request);
        return UserResponse.from(user);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        requireManageUsers(token);
        User user = userService.update(id, request);
        return UserResponse.from(user);
    }
}
