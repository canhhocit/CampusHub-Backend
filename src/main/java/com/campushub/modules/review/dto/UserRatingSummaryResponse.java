package com.campushub.modules.review.dto;

public class UserRatingSummaryResponse {
    private Long userId;
    private Double averageRating;
    private long totalReviews;

    public UserRatingSummaryResponse() {}

    public UserRatingSummaryResponse(Long userId, Double averageRating, long totalReviews) {
        this.userId = userId;
        this.averageRating = averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0;
        this.totalReviews = totalReviews;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }
}

