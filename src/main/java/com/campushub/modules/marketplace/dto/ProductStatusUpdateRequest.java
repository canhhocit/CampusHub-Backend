package com.campushub.modules.marketplace.dto;

import com.campushub.modules.marketplace.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public class ProductStatusUpdateRequest {
    
    @NotNull
    private ProductStatus status;

    @NotNull
    private Long version;

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
