package com.hsf.e_comerce.common.service;

import com.hsf.e_comerce.common.dto.response.AdminDashboardData;
import com.hsf.e_comerce.common.dto.response.TopShopDashboardItem;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.platform.dto.request.CommissionFilterRequest;
import com.hsf.e_comerce.platform.dto.response.CommissionByCategoryResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionResponse;
import com.hsf.e_comerce.platform.service.CommissionService;
import com.hsf.e_comerce.platform.service.CommissionStatisticsService;
import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderService orderService;
    private final UserService userService;
    private final ShopService shopService;
    private final CommissionService commissionService;
    private final CommissionStatisticsService commissionStatisticsService;

    /**
     * Tổng hợp dữ liệu dashboard admin cho kỳ fromDate–toDate.
     */
    public AdminDashboardData getDashboardData(LocalDate fromDate, LocalDate toDate) {
        LocalDate endDate = toDate != null ? toDate : LocalDate.now();
        LocalDate startDate = fromDate != null ? fromDate : endDate.minusDays(30);

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(daysBetween);

        // Khởi tạo
        BigDecimal revenueCurrent = BigDecimal.ZERO;
        BigDecimal revenuePrev = BigDecimal.ZERO;
        long ordersCurrent = 0;

        long countDelivered = 0, countConfirmed = 0, countProcessing = 0,
                countShipping = 0, countPendingPayment = 0, countCancelled = 0;

        Map<String, BigDecimal> revenueCurrentByShop = new HashMap<>();
        Map<String, Long> ordersCurrentByShop = new HashMap<>();
        Map<String, BigDecimal> revenuePrevByShop = new HashMap<>();
        Map<String, BigDecimal> topBuyersMap = new HashMap<>();

        Map<LocalDate, BigDecimal> revenueMap = new TreeMap<>();
        Map<LocalDate, BigDecimal> commissionMap = new TreeMap<>();
        Map<LocalDate, Long> orderCountMap = new TreeMap<>();
        Map<LocalDate, Long> userCountMap = new TreeMap<>();

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            revenueMap.put(cursor, BigDecimal.ZERO);
            commissionMap.put(cursor, BigDecimal.ZERO);
            orderCountMap.put(cursor, 0L);
            userCountMap.put(cursor, 0L);
            cursor = cursor.plusDays(1);
        }

        // Lọc đơn hàng + fix: populate revenuePrevByShop
        List<OrderResponse> recentOrders = new ArrayList<>();
        try {
            List<OrderResponse> orders = orderService.getAllOrders();

            recentOrders = orders.stream()
                    .filter(o -> o.getCreatedAt() != null)
                    .sorted(Comparator.comparing(OrderResponse::getCreatedAt).reversed())
                    .limit(7)
                    .collect(Collectors.toList());

            for (OrderResponse o : orders) {
                LocalDate createdDate = o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate() : null;
                if (createdDate == null) continue;

                // Order volume & status: theo ngày tạo đơn
                if (!createdDate.isBefore(startDate) && !createdDate.isAfter(endDate)) {
                    if (o.getStatus() == OrderStatus.DELIVERED) countDelivered++;
                    else if (o.getStatus() == OrderStatus.CONFIRMED) countConfirmed++;
                    else if (o.getStatus() == OrderStatus.PROCESSING) countProcessing++;
                    else if (o.getStatus() == OrderStatus.SHIPPING) countShipping++;
                    else if (o.getStatus() == OrderStatus.PENDING_PAYMENT) countPendingPayment++;
                    else if (o.getStatus() == OrderStatus.CANCELLED) countCancelled++;

                    orderCountMap.put(createdDate, orderCountMap.getOrDefault(createdDate, 0L) + 1);

                    if (o.getStatus() == OrderStatus.DELIVERED) {
                        // Cashflow/revenue: theo ngày giao (nếu có), fallback createdAt
                        LocalDate revenueDate = o.getDeliveredAt() != null ? o.getDeliveredAt().toLocalDate() : createdDate;
                        BigDecimal net = (o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                                .subtract(o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO);
                        revenueCurrent = revenueCurrent.add(net);
                        ordersCurrent++;

                        String shop = o.getShopName() != null ? o.getShopName() : "Unknown";
                        revenueCurrentByShop.put(shop, revenueCurrentByShop.getOrDefault(shop, BigDecimal.ZERO).add(net));
                        ordersCurrentByShop.put(shop, ordersCurrentByShop.getOrDefault(shop, 0L) + 1);

                        String buyer = o.getUserName() != null ? o.getUserName() : "Khách ẩn danh";
                        topBuyersMap.put(buyer, topBuyersMap.getOrDefault(buyer, BigDecimal.ZERO).add(net));

                        revenueMap.put(revenueDate, revenueMap.getOrDefault(revenueDate, BigDecimal.ZERO).add(net));
                    }
                } else if (!createdDate.isBefore(prevStartDate) && !createdDate.isAfter(prevEndDate)) {
                    if (o.getStatus() == OrderStatus.DELIVERED) {
                        LocalDate revenueDate = o.getDeliveredAt() != null ? o.getDeliveredAt().toLocalDate() : createdDate;
                        BigDecimal net = (o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                                .subtract(o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO);
                        revenuePrev = revenuePrev.add(net);
                        String shop = o.getShopName() != null ? o.getShopName() : "Unknown";
                        revenuePrevByShop.put(shop, revenuePrevByShop.getOrDefault(shop, BigDecimal.ZERO).add(net));
                    }
                }
            }
        } catch (Exception ignored) {}

        // User đăng ký trong kỳ
        long totalUsers = 0;
        long totalShops = 0;
        try {
            List<User> allUsers = userService.getAllUsers();
            totalUsers = allUsers.size();
            totalShops = shopService.count();
            for (User u : allUsers) {
                LocalDate regDate = u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate() : null;
                if (regDate != null && !regDate.isBefore(startDate) && !regDate.isAfter(endDate)) {
                    userCountMap.put(regDate, userCountMap.getOrDefault(regDate, 0L) + 1);
                }
            }
        } catch (Exception ignored) {}

        // Hoa hồng
        BigDecimal commissionCurrent = BigDecimal.ZERO;
        BigDecimal commissionPrev = BigDecimal.ZERO;
        try {
            List<CommissionResponse> commissions = commissionService.getCommissions(new CommissionFilterRequest());
            for (CommissionResponse c : commissions) {
                LocalDate d = c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : null;
                if (d == null) continue;
                BigDecimal amount = c.getCommissionAmount() != null ? c.getCommissionAmount() : BigDecimal.ZERO;
                if (!d.isBefore(startDate) && !d.isAfter(endDate)) {
                    commissionCurrent = commissionCurrent.add(amount);
                    commissionMap.put(d, commissionMap.getOrDefault(d, BigDecimal.ZERO).add(amount));
                } else if (!d.isBefore(prevStartDate) && !d.isAfter(prevEndDate)) {
                    commissionPrev = commissionPrev.add(amount);
                }
            }
        } catch (Exception ignored) {}

        // AOV & growth
        BigDecimal aovCurrent = ordersCurrent > 0
                ? revenueCurrent.divide(BigDecimal.valueOf(ordersCurrent), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal revenueGrowth = revenuePrev.compareTo(BigDecimal.ZERO) > 0
                ? revenueCurrent.subtract(revenuePrev).divide(revenuePrev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        BigDecimal commissionGrowth = commissionPrev.compareTo(BigDecimal.ZERO) > 0
                ? commissionCurrent.subtract(commissionPrev).divide(commissionPrev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

        // Top shops (revenuePrevByShop đã được gán đúng)
        List<TopShopDashboardItem> top5Shops = new ArrayList<>();
        TopShopDashboardItem fastestGrowthShop = null;
        try {
            List<TopShopDashboardItem> topShops = new ArrayList<>();
            for (String shop : revenueCurrentByShop.keySet()) {
                BigDecimal thisP = revenueCurrentByShop.get(shop);
                BigDecimal lastP = revenuePrevByShop.getOrDefault(shop, BigDecimal.ZERO);
                BigDecimal growth = lastP.compareTo(BigDecimal.ZERO) > 0
                        ? thisP.subtract(lastP).divide(lastP, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                topShops.add(new TopShopDashboardItem(shop, thisP, lastP, growth, ordersCurrentByShop.getOrDefault(shop, 0L)));
            }
            top5Shops = topShops.stream()
                    .sorted(Comparator.comparing(TopShopDashboardItem::getRevenueThisMonth).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            fastestGrowthShop = topShops.stream()
                    .max(Comparator.comparing(TopShopDashboardItem::getGrowthPercent))
                    .orElse(null);
        } catch (Exception ignored) {}

        List<Map.Entry<String, BigDecimal>> top100Buyers = topBuyersMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(100)
                .collect(Collectors.toList());

        LocalDate baseDate = LocalDate.now();

        List<CommissionByCategoryResponse> categoryStats = Collections.emptyList();
        try {
            categoryStats = commissionStatisticsService.getByCategory();
        } catch (Exception ignored) {}

        return AdminDashboardData.builder()
                .trendLabels(new ArrayList<>(revenueMap.keySet().stream().map(LocalDate::toString).collect(Collectors.toList())))
                .trendRevenue(new ArrayList<>(revenueMap.values()))
                .trendCommission(new ArrayList<>(commissionMap.values()))
                .trendOrderCounts(new ArrayList<>(orderCountMap.values()))
                .trendUsers(new ArrayList<>(userCountMap.values()))
                .countDelivered(countDelivered)
                .countConfirmed(countConfirmed)
                .countProcessing(countProcessing)
                .countShipping(countShipping)
                .countPendingPayment(countPendingPayment)
                .countCancelled(countCancelled)
                .aovThisMonth(aovCurrent)
                .revenueThisMonth(revenueCurrent)
                .revenueGrowth(revenueGrowth)
                .ordersThisMonth(ordersCurrent)
                .commissionThisMonth(commissionCurrent)
                .commissionGrowth(commissionGrowth)
                .top5Shops(top5Shops)
                .fastestGrowthShop(fastestGrowthShop)
                .topBuyers(top100Buyers)
                .recentOrders(recentOrders)
                .totalUsers(totalUsers)
                .totalShops(totalShops)
                .fromDate(fromDate)
                .toDate(toDate)
                .urlLast1("/admin/dashboard?fromDate=" + baseDate + "&toDate=" + baseDate)
                .urlLast7("/admin/dashboard?fromDate=" + baseDate.minusDays(7) + "&toDate=" + baseDate)
                .urlLast30("/admin/dashboard?fromDate=" + baseDate.minusDays(30) + "&toDate=" + baseDate)
                .urlLast90("/admin/dashboard?fromDate=" + baseDate.minusDays(90) + "&toDate=" + baseDate)
                .categoryStats(categoryStats)
                .build();
    }
}
