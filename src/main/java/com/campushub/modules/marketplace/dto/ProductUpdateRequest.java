package com.campushub.modules.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ProductUpdateRequest {
    
    @NotBlank
    @Size(max = 150)
    private String title;
    
    @Size(max = 2000)
    private String description;
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
    private BigDecimal price;
    
    @Size(max = 500)
    @jakarta.validation.constraints.Pattern(regexp = "(?i)^https?://[^\\s]+$", message = "Image URL must use HTTP or HTTPS")
    private String imageUrl;
    
    @NotNull
    private Long categoryId;

    @NotNull
    private Long version;

    private com.campushub.modules.marketplace.entity.ListingType listingType = com.campushub.modules.marketplace.entity.ListingType.SALE;

    @DecimalMin(value = "0.0", inclusive = true)
    @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
    private BigDecimal rentPricePerDay;

    @DecimalMin(value = "0.0", inclusive = true)
    @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
    private BigDecimal depositAmount;

    @Size(max = 500)
    @jakarta.validation.constraints.Pattern(regexp = "(?i)^https?://[^\\s]+$", message = "Video URL must use HTTP or HTTPS")
    private String videoUrl;

    private java.util.List<@Size(max = 500) String> detailImageUrls;

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public java.util.List<String> getDetailImageUrls() { return detailImageUrls; }
    public void setDetailImageUrls(java.util.List<String> detailImageUrls) { this.detailImageUrls = detailImageUrls; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public com.campushub.modules.marketplace.entity.ListingType getListingType() { return listingType; }
    public void setListingType(com.campushub.modules.marketplace.entity.ListingType listingType) { this.listingType = listingType; }
    public BigDecimal getRentPricePerDay() { return rentPricePerDay; }
    public void setRentPricePerDay(BigDecimal rentPricePerDay) { this.rentPricePerDay = rentPricePerDay; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
}
