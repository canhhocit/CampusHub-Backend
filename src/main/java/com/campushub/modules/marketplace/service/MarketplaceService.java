package com.campushub.modules.marketplace.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.marketplace.dto.*;
import com.campushub.modules.marketplace.entity.Category;
import com.campushub.modules.marketplace.entity.Product;
import com.campushub.modules.marketplace.entity.ProductStatus;
import com.campushub.modules.marketplace.repository.CategoryRepository;
import com.campushub.modules.marketplace.repository.ProductRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketplaceService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final com.campushub.modules.order.repository.OrderRepository orderRepository;
    private final com.campushub.modules.admin.service.SystemSettingService systemSettingService;

    public MarketplaceService(ProductRepository productRepository, 
                              CategoryRepository categoryRepository, 
                              UserRepository userRepository,
                              com.campushub.modules.order.repository.OrderRepository orderRepository,
                              com.campushub.modules.admin.service.SystemSettingService systemSettingService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.systemSettingService = systemSettingService;
    }

    private CategoryResponse mapCategory(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }

    private ProductResponse mapProduct(Product product) {
        ProductResponse res = new ProductResponse();
        res.setId(product.getId());
        res.setTitle(product.getTitle());
        res.setDescription(product.getDescription());
        res.setPrice(product.getPrice());
        res.setImageUrl(product.getImageUrl());
        res.setStatus(product.getStatus());
        res.setListingType(product.getListingType());
        res.setRentPricePerDay(product.getRentPricePerDay());
        res.setDepositAmount(product.getDepositAmount());
        res.setVideoUrl(product.getVideoUrl());
        if (product.getDetailImages() != null && !product.getDetailImages().isEmpty()) {
            res.setDetailImageUrls(product.getDetailImages().stream()
                    .map(com.campushub.modules.marketplace.entity.ProductImage::getImageUrl)
                    .collect(Collectors.toList()));
        }
        res.setCategory(mapCategory(product.getCategory()));
        res.setOwner(new ProductResponse.OwnerResponse(
                product.getOwner().getId(),
                product.getOwner().getUsername(),
                product.getOwner().getFullName(),
                product.getOwner().isIdentityApproved()
        ));
        res.setCreatedAt(product.getCreatedAt());
        res.setUpdatedAt(product.getUpdatedAt());
        res.setVersion(product.getVersion());
        return res;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc()
                .stream().map(this::mapCategory).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Category::getName))
                .map(this::mapCategory).collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(com.campushub.modules.marketplace.dto.CategoryCreateRequest request) {
        String slug = request.getSlug().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        Category category = new Category();
        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setActive(true);
        return mapCategory(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, com.campushub.modules.marketplace.dto.CategoryUpdateRequest request) {
        Category category = categoryRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(request.getName().trim());
        return mapCategory(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse toggleCategory(Long id) {
        Category category = categoryRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setActive(!category.getActive());
        return mapCategory(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProducts(Long categoryId, String search, Pageable pageable) {
        String searchPattern = (search == null || search.trim().isEmpty()) ? null : "%" + search.trim() + "%";
        return productRepository.findAllPublicProducts(ProductStatus.ACTIVE, categoryId, searchPattern, pageable)
                .map(this::mapProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublicProduct(Long id, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            if (username == null || !product.getOwner().getUsername().equals(username)) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
        }
        return mapProduct(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyProducts(String username, ProductStatus status, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return productRepository.findMyProducts(user.getId(), status, pageable)
                .map(this::mapProduct);
    }

    private void validateMediaLimits(java.util.List<String> detailImageUrls, String videoUrl) {
        com.campushub.modules.admin.dto.ProductMediaSettingsResponse mediaConfig = systemSettingService.getMediaConfig();
        if (detailImageUrls != null && detailImageUrls.size() > mediaConfig.getMaxProductImages()) {
            throw new AppException(ErrorCode.MAX_IMAGES_EXCEEDED);
        }
        if (videoUrl != null && !videoUrl.isBlank()) {
            if (!Boolean.TRUE.equals(mediaConfig.getAllowProductVideo())) {
                throw new AppException(ErrorCode.VIDEO_UPLOAD_NOT_ALLOWED);
            }
        }
    }

    @Transactional(readOnly = true)
    public com.campushub.modules.admin.dto.ProductMediaSettingsResponse getMediaConfig() {
        return systemSettingService.getMediaConfig();
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != com.campushub.common.enums.Role.ADMIN && !user.isIdentityApproved()) {
            throw new AppException(ErrorCode.IDENTITY_NOT_VERIFIED);
        }
                
        Category category = categoryRepository.findByIdAndIsActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        validateMediaLimits(request.getDetailImageUrls(), request.getVideoUrl());

        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setVideoUrl(request.getVideoUrl());
        product.setCategory(category);
        product.setOwner(user);
        product.setStatus(ProductStatus.ACTIVE);
        if (request.getListingType() != null) {
            product.setListingType(request.getListingType());
        }
        product.setRentPricePerDay(request.getRentPricePerDay());
        product.setDepositAmount(request.getDepositAmount());

        if (request.getDetailImageUrls() != null) {
            for (int i = 0; i < request.getDetailImageUrls().size(); i++) {
                String imgUrl = request.getDetailImageUrls().get(i);
                if (imgUrl != null && !imgUrl.isBlank()) {
                    product.addDetailImage(new com.campushub.modules.marketplace.entity.ProductImage(product, imgUrl.trim(), i));
                }
            }
        }

        product = productRepository.save(product);
        return mapProduct(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request, String username) {
        Product product = productRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getOwner().getUsername().equals(username)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }
        validateEditable(product);

        if (!product.getVersion().equals(request.getVersion())) {
            throw new AppException(ErrorCode.CONCURRENT_UPDATE);
        }

        Category category = categoryRepository.findByIdAndIsActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        validateMediaLimits(request.getDetailImageUrls(), request.getVideoUrl());

        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setVideoUrl(request.getVideoUrl());
        product.setCategory(category);
        if (request.getListingType() != null) {
            product.setListingType(request.getListingType());
        }
        product.setRentPricePerDay(request.getRentPricePerDay());
        product.setDepositAmount(request.getDepositAmount());

        if (request.getDetailImageUrls() != null) {
            product.clearDetailImages();
            for (int i = 0; i < request.getDetailImageUrls().size(); i++) {
                String imgUrl = request.getDetailImageUrls().get(i);
                if (imgUrl != null && !imgUrl.isBlank()) {
                    product.addDetailImage(new com.campushub.modules.marketplace.entity.ProductImage(product, imgUrl.trim(), i));
                }
            }
        }

        product = productRepository.saveAndFlush(product);
        return mapProduct(product);
    }

    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatusUpdateRequest request, String username) {
        Product product = productRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getOwner().getUsername().equals(username)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }
        validateEditable(product);

        if (!product.getVersion().equals(request.getVersion())) {
            throw new AppException(ErrorCode.CONCURRENT_UPDATE);
        }

        ProductStatus current = product.getStatus();
        ProductStatus target = request.getStatus();

        if (current == ProductStatus.SOLD && target != ProductStatus.SOLD) {
            throw new AppException(ErrorCode.INVALID_PRODUCT_STATUS);
        }

        if (current == ProductStatus.HIDDEN && target == ProductStatus.ACTIVE) {
            if (!product.getCategory().getActive()) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND); // Category inactive
            }
        }

        product.setStatus(target);

        product = productRepository.saveAndFlush(product);
        return mapProduct(product);
    }

    private void validateEditable(Product product) {
        if (product.getStatus() == ProductStatus.SOLD) {
            throw new AppException(ErrorCode.INVALID_PRODUCT_STATUS);
        }
        if (orderRepository.hasProductReservation(product.getId(), java.util.Set.of(
                com.campushub.modules.order.entity.OrderStatus.PENDING_SELLER,
                com.campushub.modules.order.entity.OrderStatus.AWAITING_DEPOSIT,
                com.campushub.modules.order.entity.OrderStatus.READY_FOR_PICKUP))) {
            throw new AppException(ErrorCode.ACTIVE_ORDER_EXISTS);
        }
    }
}
