package com.campushub.modules.admin.dto;

import jakarta.validation.constraints.NotNull;

public record IdentityReviewRequest(@NotNull Boolean approved) {}
