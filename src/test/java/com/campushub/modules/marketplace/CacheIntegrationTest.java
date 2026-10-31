package com.campushub.modules.marketplace;

import com.campushub.modules.admin.service.SystemSettingService;
import com.campushub.modules.marketplace.dto.CategoryCreateRequest;
import com.campushub.modules.marketplace.dto.CategoryResponse;
import com.campushub.modules.marketplace.service.MarketplaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
public class CacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MarketplaceService marketplaceService;

    @Autowired
    private SystemSettingService systemSettingService;

    @Test
    @DisplayName("Category caching: getActiveCategories caches list and createCategory evicts cache")
    void testCategoryCachingAndEviction() {
        Cache cache = cacheManager.getCache("categories");
        assertNotNull(cache, "Cache 'categories' must be defined in CacheManager");

        // Clear cache before test
        cache.clear();
        assertNull(cache.get("active"));

        // 1. Fetch categories: should populate cache
        List<CategoryResponse> firstFetch = marketplaceService.getActiveCategories();
        assertNotNull(firstFetch);
        assertNotNull(cache.get("active"), "Cache 'categories' with key 'active' should now be populated");

        // 2. Fetch again: should come from cache
        List<CategoryResponse> secondFetch = marketplaceService.getActiveCategories();
        assertEquals(firstFetch.size(), secondFetch.size());

        // 3. Mutate: create new category -> should trigger @CacheEvict
        CategoryCreateRequest createReq = new CategoryCreateRequest();
        createReq.setName("Test Cache Eviction Category " + System.currentTimeMillis());
        createReq.setSlug("test-cache-" + System.currentTimeMillis());
        marketplaceService.createCategory(createReq);

        // 4. Verify cache was evicted
        assertNull(cache.get("active"), "Cache 'categories' should be evicted after createCategory");
    }

    @Test
    @DisplayName("SystemSetting caching: getMinWithdrawalAmount caches value and update evicts cache")
    void testSystemSettingCachingAndEviction() {
        Cache cache = cacheManager.getCache("system_settings");
        assertNotNull(cache, "Cache 'system_settings' must be defined in CacheManager");

        // Clear cache
        cache.clear();
        assertNull(cache.get("min_withdrawal"));

        // 1. Fetch setting: should populate cache
        BigDecimal minWithdrawal = systemSettingService.getMinWithdrawalAmount();
        assertNotNull(minWithdrawal);
        assertNotNull(cache.get("min_withdrawal"), "Cache 'system_settings' with key 'min_withdrawal' should be populated");

        // 2. Update setting: should evict cache
        systemSettingService.updateMinWithdrawalAmount(new BigDecimal("75000.00"));

        // 3. Verify eviction
        assertNull(cache.get("min_withdrawal"), "Cache 'system_settings' should be evicted after updateMinWithdrawalAmount");

        // 4. Reset to standard default for other tests
        systemSettingService.updateMinWithdrawalAmount(new BigDecimal("10000.00"));
    }
}

