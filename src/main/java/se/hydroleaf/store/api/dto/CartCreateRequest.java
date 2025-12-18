package se.hydroleaf.store.api.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartCreateRequest {
    private String sessionId;
    private UUID userId;
}
