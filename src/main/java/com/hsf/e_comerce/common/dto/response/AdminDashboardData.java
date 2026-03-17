package com.hsf.e_comerce.common.dto.response;

import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionByCategoryResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dữ liệu tổng hợp cho trang Admin Dashboard.
 */
@Data
@Builder
public class AdminDashboardData {

    private List<String> trendLabels;
    private List<BigDecimal> trendRevenue;
    private List<BigDecimal> trendCommission;
    private List<Long> trendOrderCounts;
    private List<Long> trendUsers;

    private long countDelivered;
    private long countConfirmed;
    private long countProcessing;
    private long countShipping;
    private long countPendingPayment;
    private long countCancelled;

    private BigDecimal aovThisMonth;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueGrowth;
    private long ordersThisMonth;
    private BigDecimal commissionThisMonth;
    private BigDecimal commissionGrowth;

    private List<TopShopDashboardItem> top5Shops;
    private TopShopDashboardItem fastestGrowthShop;
    private List<Map.Entry<String, BigDecimal>> topBuyers;
    private List<OrderResponse> recentOrders;

    private long totalUsers;
    private long totalShops;

    private LocalDate fromDate;
    private LocalDate toDate;
    private String urlLast1;
    private String urlLast7;
    private String urlLast30;
    private String urlLast90;

    private List<CommissionByCategoryResponse> categoryStats;
}
