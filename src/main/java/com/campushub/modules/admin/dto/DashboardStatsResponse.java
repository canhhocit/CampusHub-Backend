package com.campushub.modules.admin.dto;

public class DashboardStatsResponse {

    private UserStats userStats;
    private ProductStats productStats;
    private OrderStats orderStats;
    private LockerStats lockerStats;
    private SecurityStats securityStats;

    public static class UserStats {
        private long totalUsers;
        private long activeUsers;
        private long pendingUsers;
        private long lockedUsers;
        private long newUsersToday;
        private long newUsersThisWeek;

        // Getters and Setters
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
        public long getPendingUsers() { return pendingUsers; }
        public void setPendingUsers(long pendingUsers) { this.pendingUsers = pendingUsers; }
        public long getLockedUsers() { return lockedUsers; }
        public void setLockedUsers(long lockedUsers) { this.lockedUsers = lockedUsers; }
        public long getNewUsersToday() { return newUsersToday; }
        public void setNewUsersToday(long newUsersToday) { this.newUsersToday = newUsersToday; }
        public long getNewUsersThisWeek() { return newUsersThisWeek; }
        public void setNewUsersThisWeek(long newUsersThisWeek) { this.newUsersThisWeek = newUsersThisWeek; }
    }

    public static class ProductStats {
        private long totalProducts;
        private long activeProducts;
        private long soldProducts;
        private long hiddenProducts;
        private long newProductsToday;
        private long newProductsThisWeek;

        // Getters and Setters
        public long getTotalProducts() { return totalProducts; }
        public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }
        public long getActiveProducts() { return activeProducts; }
        public void setActiveProducts(long activeProducts) { this.activeProducts = activeProducts; }
        public long getSoldProducts() { return soldProducts; }
        public void setSoldProducts(long soldProducts) { this.soldProducts = soldProducts; }
        public long getHiddenProducts() { return hiddenProducts; }
        public void setHiddenProducts(long hiddenProducts) { this.hiddenProducts = hiddenProducts; }
        public long getNewProductsToday() { return newProductsToday; }
        public void setNewProductsToday(long newProductsToday) { this.newProductsToday = newProductsToday; }
        public long getNewProductsThisWeek() { return newProductsThisWeek; }
        public void setNewProductsThisWeek(long newProductsThisWeek) { this.newProductsThisWeek = newProductsThisWeek; }
    }

    public static class OrderStats {
        private long totalOrders;
        private long pendingOrders;
        private long awaitingDepositOrders;
        private long readyForPickupOrders;
        private long completedOrders;
        private long expiredOrders;
        private long cancelledOrders;
        private long rejectedOrders;
        private long ordersToday;
        private long ordersThisWeek;
        private long completedOrdersToday;

        // Getters and Setters
        public long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }
        public long getAwaitingDepositOrders() { return awaitingDepositOrders; }
        public void setAwaitingDepositOrders(long awaitingDepositOrders) { this.awaitingDepositOrders = awaitingDepositOrders; }
        public long getReadyForPickupOrders() { return readyForPickupOrders; }
        public void setReadyForPickupOrders(long readyForPickupOrders) { this.readyForPickupOrders = readyForPickupOrders; }
        public long getCompletedOrders() { return completedOrders; }
        public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }
        public long getExpiredOrders() { return expiredOrders; }
        public void setExpiredOrders(long expiredOrders) { this.expiredOrders = expiredOrders; }
        public long getCancelledOrders() { return cancelledOrders; }
        public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }
        public long getRejectedOrders() { return rejectedOrders; }
        public void setRejectedOrders(long rejectedOrders) { this.rejectedOrders = rejectedOrders; }
        public long getOrdersToday() { return ordersToday; }
        public void setOrdersToday(long ordersToday) { this.ordersToday = ordersToday; }
        public long getOrdersThisWeek() { return ordersThisWeek; }
        public void setOrdersThisWeek(long ordersThisWeek) { this.ordersThisWeek = ordersThisWeek; }
        public long getCompletedOrdersToday() { return completedOrdersToday; }
        public void setCompletedOrdersToday(long completedOrdersToday) { this.completedOrdersToday = completedOrdersToday; }
    }

    public static class LockerStats {
        private long totalLockers;
        private long activeLockers;
        private long totalCompartments;
        private long availableCompartments;
        private long reservedCompartments;
        private long occupiedCompartments;
        private double utilizationRate;

        // Getters and Setters
        public long getTotalLockers() { return totalLockers; }
        public void setTotalLockers(long totalLockers) { this.totalLockers = totalLockers; }
        public long getActiveLockers() { return activeLockers; }
        public void setActiveLockers(long activeLockers) { this.activeLockers = activeLockers; }
        public long getTotalCompartments() { return totalCompartments; }
        public void setTotalCompartments(long totalCompartments) { this.totalCompartments = totalCompartments; }
        public long getAvailableCompartments() { return availableCompartments; }
        public void setAvailableCompartments(long availableCompartments) { this.availableCompartments = availableCompartments; }
        public long getReservedCompartments() { return reservedCompartments; }
        public void setReservedCompartments(long reservedCompartments) { this.reservedCompartments = reservedCompartments; }
        public long getOccupiedCompartments() { return occupiedCompartments; }
        public void setOccupiedCompartments(long occupiedCompartments) { this.occupiedCompartments = occupiedCompartments; }
        public double getUtilizationRate() { return utilizationRate; }
        public void setUtilizationRate(double utilizationRate) { this.utilizationRate = utilizationRate; }
    }

    public static class SecurityStats {
        private long failedLoginsToday;
        private long failedLoginsThisWeek;
        private long failedPickupAttemptsToday;
        private long expiredTokensCleanedUp;

        // Getters and Setters
        public long getFailedLoginsToday() { return failedLoginsToday; }
        public void setFailedLoginsToday(long failedLoginsToday) { this.failedLoginsToday = failedLoginsToday; }
        public long getFailedLoginsThisWeek() { return failedLoginsThisWeek; }
        public void setFailedLoginsThisWeek(long failedLoginsThisWeek) { this.failedLoginsThisWeek = failedLoginsThisWeek; }
        public long getFailedPickupAttemptsToday() { return failedPickupAttemptsToday; }
        public void setFailedPickupAttemptsToday(long failedPickupAttemptsToday) { this.failedPickupAttemptsToday = failedPickupAttemptsToday; }
        public long getExpiredTokensCleanedUp() { return expiredTokensCleanedUp; }
        public void setExpiredTokensCleanedUp(long expiredTokensCleanedUp) { this.expiredTokensCleanedUp = expiredTokensCleanedUp; }
    }

    // Main getters and setters
    public UserStats getUserStats() { return userStats; }
    public void setUserStats(UserStats userStats) { this.userStats = userStats; }
    public ProductStats getProductStats() { return productStats; }
    public void setProductStats(ProductStats productStats) { this.productStats = productStats; }
    public OrderStats getOrderStats() { return orderStats; }
    public void setOrderStats(OrderStats orderStats) { this.orderStats = orderStats; }
    public LockerStats getLockerStats() { return lockerStats; }
    public void setLockerStats(LockerStats lockerStats) { this.lockerStats = lockerStats; }
    public SecurityStats getSecurityStats() { return securityStats; }
    public void setSecurityStats(SecurityStats securityStats) { this.securityStats = securityStats; }
}
