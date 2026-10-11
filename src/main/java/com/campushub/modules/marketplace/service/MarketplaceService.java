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

    public MarketplaceService(ProductRepository productRepository, 
                              CategoryRepository categoryRepository, 
                              UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
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
        res.setCategory(mapCategory(product.getCategory()));
        res.setOwner(new ProductResponse.OwnerResponse(product.getOwner().getId(), product.getOwner().getUsername(), product.getOwner().getFullName()));
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
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(request.getName().trim());
        return mapCategory(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse toggleCategory(Long id) {
        Category category = categoryRepository.findById(id)
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
    public Page<ProductResponse> getMyProducts(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return productRepository.findAllByOwnerId(user.getId(), pageable)
                .map(this::mapProduct);
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                
        Category category = categoryRepository.findByIdAndIsActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setOwner(user);
        product.setStatus(ProductStatus.ACTIVE);

        product = productRepository.save(product);
        return mapProduct(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getOwner().getUsername().equals(username)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (!product.getVersion().equals(request.getVersion())) {
            throw new AppException(ErrorCode.CONCURRENT_UPDATE);
        }

        Category category = categoryRepository.findByIdAndIsActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);

        product = productRepository.saveAndFlush(product);
        return mapProduct(product);
    }

    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatusUpdateRequest request, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getOwner().getUsername().equals(username)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (!product.getVersion().equals(request.getVersion())) {
            throw new AppException(ErrorCode.CONCURRENT_UPDATE);
        }

        ProductStatus current = product.getStatus();
        ProductStatus target = request.getStatus();

        if (current == ProductStatus.SOLD && target == ProductStatus.ACTIVE) {
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
}
