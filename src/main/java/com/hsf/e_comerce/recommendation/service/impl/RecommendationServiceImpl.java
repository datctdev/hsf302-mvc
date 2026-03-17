package com.hsf.e_comerce.recommendation.service.impl;

import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.repository.ProductRepository;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.recommendation.entity.ProductEmbedding;
import com.hsf.e_comerce.recommendation.entity.SearchHistory;
import com.hsf.e_comerce.recommendation.repository.ProductEmbeddingRepository;
import com.hsf.e_comerce.recommendation.repository.ProductViewRepository;
import com.hsf.e_comerce.recommendation.repository.SearchHistoryRepository;
import com.hsf.e_comerce.recommendation.service.ProductEmbeddingService;
import com.hsf.e_comerce.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int RECENT_SEARCH_LIMIT = 5;
    private static final int RECENT_VIEW_LIMIT = 15;
    private static final int SIMILAR_CANDIDATES_PAGE_SIZE = 50;

    private final SearchHistoryRepository searchHistoryRepository;
    private final ProductViewRepository productViewRepository;
    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ProductEmbeddingService productEmbeddingService;

    @Override
    @Transactional
    @Async
    public void recordSearch(String sessionId, UUID userId, String keyword, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        if (sessionId == null || sessionId.isBlank()) return;
        try {
            SearchHistory sh = SearchHistory.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .keyword(keyword != null && !keyword.isBlank() ? keyword.trim() : null)
                    .categoryId(categoryId)
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .createdAt(LocalDateTime.now())
                    .build();
            searchHistoryRepository.save(sh);
        } catch (Exception e) {
            log.warn("Failed to record search: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    @Async
    public void recordProductView(String sessionId, UUID userId, UUID productId) {
        if (sessionId == null || sessionId.isBlank() || productId == null) return;
        try {
            var pv = com.hsf.e_comerce.recommendation.entity.ProductView.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .productId(productId)
                    .viewedAt(LocalDateTime.now())
                    .build();
            productViewRepository.save(pv);
        } catch (Exception e) {
            log.warn("Failed to record product view: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getRecommendationsForUser(String sessionId, UUID userId, int limit) {
        if (limit <= 0) limit = 8;
        Set<UUID> seen = new LinkedHashSet<>();
        List<ProductResponse> out = new ArrayList<>();

        // 1. Từ lịch sử tìm kiếm: lấy vài keyword/category gần nhất, search lại
        List<SearchHistory> recentSearches = searchHistoryRepository.findRecentBySessionOrUser(
                sessionId != null ? sessionId : "",
                userId,
                PageRequest.of(0, RECENT_SEARCH_LIMIT)
        );
        for (SearchHistory sh : recentSearches) {
            String kw = sh.getKeyword() != null && !sh.getKeyword().isBlank() ? sh.getKeyword() : null;
            if (kw == null && sh.getCategoryId() == null && sh.getMinPrice() == null && sh.getMaxPrice() == null) continue;
            var page = productService.searchForChatbot(kw, sh.getMinPrice(), sh.getMaxPrice(), sh.getCategoryId(), 0, 4);
            for (ProductResponse p : page.getContent()) {
                if (seen.add(p.getId())) {
                    out.add(p);
                    if (out.size() >= limit) return out;
                }
            }
        }

        // 2. Từ đã xem: lấy sản phẩm cùng category
        List<com.hsf.e_comerce.recommendation.entity.ProductView> recentViews = productViewRepository.findRecentBySessionOrUser(
                sessionId != null ? sessionId : "",
                userId,
                PageRequest.of(0, RECENT_VIEW_LIMIT)
        );
        for (com.hsf.e_comerce.recommendation.entity.ProductView v : recentViews) {
            List<ProductResponse> similar = getSimilarProducts(v.getProductId(), 3);
            for (ProductResponse p : similar) {
                if (seen.add(p.getId())) {
                    out.add(p);
                    if (out.size() >= limit) return out;
                }
            }
        }

        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getSimilarProducts(UUID productId, int limit) {
        if (limit <= 0) limit = 8;
        Optional<Product> opt = productRepository.findPublishedById(productId);
        if (opt.isEmpty()) return List.of();

        Product product = opt.get();
        UUID categoryId = product.getCategory() != null ? product.getCategory().getId() : null;

        float[] sourceVec = productEmbeddingService.getOrComputeEmbedding(product);
        List<Product> candidates = productRepository.findPublishedByCategoryExcludingId(
                productId, categoryId, PageRequest.of(0, SIMILAR_CANDIDATES_PAGE_SIZE));

        if (candidates.isEmpty()) return List.of();

        if (sourceVec == null) {
            return candidates.stream().limit(limit)
                    .map(p -> productService.getPublishedProductById(p.getId()))
                    .collect(Collectors.toList());
        }

        List<ProductEmbedding> embeddings = productEmbeddingRepository.findByProductIdIn(
                candidates.stream().map(Product::getId).toList());
        Map<UUID, float[]> vecMap = new HashMap<>();
        for (ProductEmbedding pe : embeddings) {
            float[] v = productEmbeddingService.getStoredEmbedding(pe.getProductId());
            if (v != null) vecMap.put(pe.getProductId(), v);
        }
        for (Product p : candidates) {
            if (vecMap.containsKey(p.getId())) continue;
            float[] v = productEmbeddingService.getOrComputeEmbedding(p);
            if (v != null) vecMap.put(p.getId(), v);
        }

        List<UUID> sorted = candidates.stream()
                .map(Product::getId)
                .filter(vecMap::containsKey)
                .sorted(Comparator.comparingDouble(id -> -cosineSimilarity(sourceVec, vecMap.get(id))))
                .limit(limit)
                .toList();

        if (sorted.isEmpty()) {
            return candidates.stream().limit(limit)
                    .map(p -> productService.getPublishedProductById(p.getId()))
                    .collect(Collectors.toList());
        }
        List<ProductResponse> byIds = productService.getPublishedProductsByIds(sorted);
        Map<UUID, ProductResponse> byIdMap = byIds.stream().collect(Collectors.toMap(ProductResponse::getId, p -> p));
        return sorted.stream().map(byIdMap::get).filter(Objects::nonNull).toList();
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
