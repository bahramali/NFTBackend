package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;
import se.hydroleaf.model.Permission;

public record AdminPermissionsUpdateRequest(@NotNull Set<Permission> permissions) {
}
