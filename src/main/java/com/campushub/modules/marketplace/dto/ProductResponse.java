package com.campushub.modules.marketplace.dto;

import com.campushub.modules.marketplace.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private ProductStatus status;
    private com.campushub.modules.marketplace.entity.ListingType listingType = com.campushub.modules.marketplace.entity.ListingType.SALE;
    private BigDecimal rentPricePerDay;
    private BigDecimal depositAmount;
    private String videoUrl;
    private java.util.List<String> detailImageUrls = new java.util.ArrayList<>();
    private CategoryResponse category;
    private OwnerResponse owner;
    private LockerSummaryResponse locker;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public static class LockerSummaryResponse {
        private Long id;
        private String code;
        private String name;
        private String location;

        public LockerSummaryResponse() {}
        public LockerSummaryResponse(Long id, String code, String name, String location) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.location = location;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    public LockerSummaryResponse getLocker() { return locker; }
    public void setLocker(LockerSummaryResponse locker) { this.locker = locker; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public java.util.List<String> getDetailImageUrls() { return detailImageUrls; }
    public void setDetailImageUrls(java.util.List<String> detailImageUrls) { this.detailImageUrls = detailImageUrls; }

    public static class OwnerResponse {
        private Long id;
        private String username;
        private String fullName;
        private Boolean identityVerified;

        public OwnerResponse(Long id, String fullName) {
            this.id = id;
            this.fullName = fullName;
        }

        public OwnerResponse(Long id, String username, String fullName) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
        }

        public OwnerResponse(Long id, String username, String fullName, Boolean identityVerified) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.identityVerified = identityVerified;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public Boolean getIdentityVerified() { return identityVerified; }
    }

    // Builder or Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public CategoryResponse getCategory() { return category; }
    public void setCategory(CategoryResponse category) { this.category = category; }
    public OwnerResponse getOwner() { return owner; }
    public void setOwner(OwnerResponse owner) { this.owner = owner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public com.campushub.modules.marketplace.entity.ListingType getListingType() { return listingType; }
    public void setListingType(com.campushub.modules.marketplace.entity.ListingType listingType) { this.listingType = listingType; }
    public BigDecimal getRentPricePerDay() { return rentPricePerDay; }
    public void setRentPricePerDay(BigDecimal rentPricePerDay) { this.rentPricePerDay = rentPricePerDay; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
}
