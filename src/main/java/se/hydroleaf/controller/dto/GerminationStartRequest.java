package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record GerminationStartRequest(@NotNull Instant startTime) {
}

