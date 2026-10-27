package com.campushub.modules.marketplace.controller;

import com.campushub.modules.marketplace.dto.*;
import com.campushub.modules.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Marketplace", description = "Product and category management APIs")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(marketplaceService.getActiveCategories());
    }

    @GetMapping("/marketplace/media-settings")
    @Operation(summary = "Get product media settings for marketplace")
    public ResponseEntity<com.campushub.modules.admin.dto.ProductMediaSettingsResponse> getMediaSettings() {
        return ResponseEntity.ok(marketplaceService.getMediaConfig());
    }

    @GetMapping("/categories/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(marketplaceService.getAllCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody com.campushub.modules.marketplace.dto.CategoryCreateRequest request) {
        return new ResponseEntity<>(marketplaceService.createCategory(request), HttpStatus.CREATED);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody com.campushub.modules.marketplace.dto.CategoryUpdateRequest request) {
        return ResponseEntity.ok(marketplaceService.updateCategory(id, request));
    }

    @PatchMapping("/categories/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> toggleCategory(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.toggleCategory(id));
    }

    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) com.campushub.modules.marketplace.entity.ListingType listingType,
            @RequestParam(required = false) Long lockerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        com.campushub.common.model.PageRequests.validateSort(sort, direction, java.util.Set.of("price", "createdAt"));
        if (size > 50) size = 50;
        
        Sort.Direction dir = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProp = sort.equals("price") ? "price" : "createdAt";
        Pageable pageable = com.campushub.common.model.PageRequests.of(page, size, Sort.by(dir, sortProp));
        
        return ResponseEntity.ok(marketplaceService.getPublicProducts(categoryId, search, minPrice, maxPrice, listingType, lockerId, pageable));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id, Authentication authentication) {
        String username = (authentication != null && authentication.isAuthenticated() 
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken))
                ? authentication.getName() 
                : null;
        return ResponseEntity.ok(marketplaceService.getPublicProduct(id, username));
    }

    @GetMapping("/products/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            Authentication authentication,
            @RequestParam(required = false) com.campushub.modules.marketplace.entity.ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (size > 50) size = 50;
        Pageable pageable = com.campushub.common.model.PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        return ResponseEntity.ok(marketplaceService.getMyProducts(authentication.getName(), status, pageable));
    }

    @PostMapping("/products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            Authentication authentication) {
        ProductResponse response = marketplaceService.createProduct(request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(marketplaceService.updateProduct(id, request, authentication.getName()));
    }

    @PatchMapping("/products/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(marketplaceService.updateProductStatus(id, request, authentication.getName()));
    }
}
