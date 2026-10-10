package com.campushub.modules.marketplace.controller;

import com.campushub.modules.marketplace.dto.*;
import com.campushub.modules.marketplace.service.MarketplaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(marketplaceService.getActiveCategories());
    }

    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        if (size > 50) size = 50;
        
        Sort.Direction dir = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProp = sort.equals("price") ? "price" : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortProp));
        
        return ResponseEntity.ok(marketplaceService.getPublicProducts(categoryId, search, pageable));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getPublicProduct(id));
    }

    @GetMapping("/products/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (size > 50) size = 50;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        return ResponseEntity.ok(marketplaceService.getMyProducts(authentication.getName(), pageable));
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
