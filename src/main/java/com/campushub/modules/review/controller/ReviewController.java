package com.campushub.modules.review.controller;

import com.campushub.modules.review.dto.ReviewCreateRequest;
import com.campushub.modules.review.dto.ReviewResponse;
import com.campushub.modules.review.dto.UserRatingSummaryResponse;
import com.campushub.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reviews", description = "Đánh giá & uy tín người bán")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/orders/{orderId}/reviews")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Người mua gửi đánh giá cho đơn hàng đã hoàn tất")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long orderId,
            @Valid @RequestBody ReviewCreateRequest request,
            Authentication authentication
    ) {
        ReviewResponse response = reviewService.createReview(orderId, request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/orders/{orderId}/reviews")
    @Operation(summary = "Lấy đánh giá của một đơn hàng cụ thể")
    public ResponseEntity<ReviewResponse> getReviewByOrder(@PathVariable Long orderId) {
        ReviewResponse response = reviewService.getReviewByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/reviews")
    @Operation(summary = "Lấy danh sách đánh giá nhận được của người bán")
    public ResponseEntity<Page<ReviewResponse>> getSellerReviews(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsBySeller(userId, pageable));
    }

    @GetMapping("/users/{userId}/rating-summary")
    @Operation(summary = "Lấy điểm uy tín trung bình và tổng số lượt đánh giá của người bán")
    public ResponseEntity<UserRatingSummaryResponse> getSellerRatingSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getSellerRatingSummary(userId));
    }

    @GetMapping("/products/{productId}/reviews")
    @Operation(summary = "Lấy danh sách đánh giá của sản phẩm")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }
}

