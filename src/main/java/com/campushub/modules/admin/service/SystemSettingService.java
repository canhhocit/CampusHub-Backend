package com.campushub.modules.admin.service;

import com.campushub.modules.admin.dto.FeeConfigResponse;
import com.campushub.modules.admin.dto.UpdateFeeConfigRequest;
import com.campushub.modules.admin.entity.FeeMode;
import com.campushub.modules.admin.entity.SystemSetting;
import com.campushub.modules.admin.repository.SystemSettingRepository;
import com.campushub.modules.order.entity.OrderType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class SystemSettingService {

    public static final String KEY_SALE_COMMISSION_PERCENT = "SALE_COMMISSION_PERCENT";
    public static final String KEY_RENTAL_FEE_MODE = "RENTAL_FEE_MODE";
    public static final String KEY_RENTAL_FEE_VALUE = "RENTAL_FEE_VALUE";
    public static final String KEY_MAX_PRODUCT_IMAGES = "MAX_PRODUCT_IMAGES";
    public static final String KEY_MAX_PRODUCT_VIDEOS = "MAX_PRODUCT_VIDEOS";
    public static final String KEY_ALLOW_PRODUCT_VIDEO = "ALLOW_PRODUCT_VIDEO";
    public static final String KEY_MAX_VIDEO_SIZE_MB = "MAX_VIDEO_SIZE_MB";
    public static final String KEY_MIN_WITHDRAWAL_AMOUNT = "MIN_WITHDRAWAL_AMOUNT";

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Transactional(readOnly = true)
    public FeeConfigResponse getFeeConfig() {
        BigDecimal salePercent = new BigDecimal(getSettingValue(KEY_SALE_COMMISSION_PERCENT, "5.0"));
        String rentalModeStr = getSettingValue(KEY_RENTAL_FEE_MODE, "PERCENT");
        FeeMode rentalMode = "PER_DAY".equalsIgnoreCase(rentalModeStr) ? FeeMode.PER_DAY : FeeMode.PERCENT;
        BigDecimal rentalValue = new BigDecimal(getSettingValue(KEY_RENTAL_FEE_VALUE, "10.0"));

        return new FeeConfigResponse(salePercent, rentalMode, rentalValue);
    }

    @Transactional
    public FeeConfigResponse updateFeeConfig(UpdateFeeConfigRequest request) {
        saveSetting(KEY_SALE_COMMISSION_PERCENT, request.getSaleCommissionPercent().stripTrailingZeros().toPlainString(), "Phần trăm hoa hồng đơn bán (%)");
        saveSetting(KEY_RENTAL_FEE_MODE, request.getRentalFeeMode().name(), "Cách tính phí thuê (PERCENT hoặc PER_DAY)");
        saveSetting(KEY_RENTAL_FEE_VALUE, request.getRentalFeeValue().stripTrailingZeros().toPlainString(), "Giá trị phí thuê");

        return getFeeConfig();
    }

    @Transactional(readOnly = true)
    public com.campushub.modules.admin.dto.ProductMediaSettingsResponse getMediaConfig() {
        int maxImages = Integer.parseInt(getSettingValue(KEY_MAX_PRODUCT_IMAGES, "5"));
        int maxVideos = Integer.parseInt(getSettingValue(KEY_MAX_PRODUCT_VIDEOS, "1"));
        boolean allowVideo = Boolean.parseBoolean(getSettingValue(KEY_ALLOW_PRODUCT_VIDEO, "true"));
        int maxVideoSizeMb = Integer.parseInt(getSettingValue(KEY_MAX_VIDEO_SIZE_MB, "50"));

        return new com.campushub.modules.admin.dto.ProductMediaSettingsResponse(maxImages, maxVideos, allowVideo, maxVideoSizeMb);
    }

    @Transactional
    public com.campushub.modules.admin.dto.ProductMediaSettingsResponse updateMediaConfig(com.campushub.modules.admin.dto.UpdateProductMediaSettingsRequest request) {
        saveSetting(KEY_MAX_PRODUCT_IMAGES, String.valueOf(request.getMaxProductImages()), "Số lượng ảnh mô tả tối đa cho mỗi sản phẩm");
        saveSetting(KEY_MAX_PRODUCT_VIDEOS, String.valueOf(request.getMaxProductVideos()), "Số lượng video tối đa cho mỗi sản phẩm");
        saveSetting(KEY_ALLOW_PRODUCT_VIDEO, String.valueOf(request.getAllowProductVideo()), "Cho phép người bán upload video sản phẩm");
        saveSetting(KEY_MAX_VIDEO_SIZE_MB, String.valueOf(request.getMaxVideoSizeMb()), "Dung lượng tối đa của video sản phẩm (MB)");

        return getMediaConfig();
    }

    public record FeeCalculation(
            BigDecimal totalAmount,
            BigDecimal depositAmount,
            BigDecimal platformFee,
            BigDecimal sellerPayout
    ) {}

    @Transactional(readOnly = true)
    public FeeCalculation calculateFees(
            OrderType orderType,
            BigDecimal salePrice,
            BigDecimal rentPricePerDay,
            BigDecimal depositAmount,
            Integer durationDays
    ) {
        FeeConfigResponse config = getFeeConfig();

        if (orderType == OrderType.PURCHASE) {
            BigDecimal price = salePrice != null ? salePrice : BigDecimal.ZERO;
            BigDecimal feePercent = config.getSaleCommissionPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal platformFee = price.multiply(feePercent).setScale(2, RoundingMode.HALF_UP);
            BigDecimal sellerPayout = price.subtract(platformFee);

            return new FeeCalculation(price, BigDecimal.ZERO, platformFee, sellerPayout);
        } else {
            // Rental calculation
            int days = durationDays != null && durationDays > 0 ? durationDays : 1;
            BigDecimal perDay = rentPricePerDay != null ? rentPricePerDay : BigDecimal.ZERO;
            BigDecimal deposit = depositAmount != null ? depositAmount : BigDecimal.ZERO;

            BigDecimal totalRent = perDay.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = totalRent.add(deposit);

            BigDecimal platformFee;
            if (config.getRentalFeeMode() == FeeMode.PERCENT) {
                BigDecimal percent = config.getRentalFeeValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                platformFee = totalRent.multiply(percent).setScale(2, RoundingMode.HALF_UP);
            } else {
                // PER_DAY
                platformFee = config.getRentalFeeValue().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
                // Ensure platform fee does not exceed total rent
                if (platformFee.compareTo(totalRent) > 0) {
                    platformFee = totalRent;
                }
            }

            BigDecimal sellerPayout = totalRent.subtract(platformFee);
            return new FeeCalculation(totalAmount, deposit, platformFee, sellerPayout);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getMinWithdrawalAmount() {
        return new BigDecimal(getSettingValue(KEY_MIN_WITHDRAWAL_AMOUNT, "50000.00"));
    }

    @Transactional
    public BigDecimal updateMinWithdrawalAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hạn mức rút tiền tối thiểu phải lớn hơn 0");
        }
        saveSetting(KEY_MIN_WITHDRAWAL_AMOUNT, amount.stripTrailingZeros().toPlainString(), "Hạn mức rút tiền ví tối thiểu (VNĐ)");
        return getMinWithdrawalAmount();
    }

    private String getSettingValue(String key, String defaultValue) {
        return systemSettingRepository.findById(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    private void saveSetting(String key, String value, String description) {
        SystemSetting setting = systemSettingRepository.findById(key)
                .orElse(new SystemSetting(key, value, description));
        setting.setValue(value);
        setting.setDescription(description);
        setting.setUpdatedAt(LocalDateTime.now());
        systemSettingRepository.save(setting);
    }
}
