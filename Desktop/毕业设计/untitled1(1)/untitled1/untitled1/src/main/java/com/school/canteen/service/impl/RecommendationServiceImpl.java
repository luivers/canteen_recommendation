package com.school.canteen.service.impl;

import com.school.canteen.entity.Dish;
import com.school.canteen.repository.DishRepository;
import com.school.canteen.repository.OrderRepository;
import com.school.canteen.service.RecommendationService;
import com.school.canteen.service.strategy.RecommendationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 菜品推荐服务实现类，整合多种推荐策略提供混合推荐 */
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final Map<String, RecommendationStrategy> strategies;
    
    @Autowired
    private DishRepository dishRepository;
    
    @Autowired
    private OrderRepository orderRepository;

    public RecommendationServiceImpl(List<RecommendationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(RecommendationStrategy::getStrategyType, s -> s));
    }

    @Override
    public List<Dish> getPersonalizedRecommendations(Long userId, int limit) {
        // Hybrid approach:
        // 1. Try Collaborative Filtering (if enough data)
        // 2. Fallback to Content-Based
        // 3. Fallback to Popularity
        
        List<Dish> recs = new ArrayList<>();
        
        // 1. Collaborative
        if (strategies.containsKey("collaborative")) {
            try {
                recs.addAll(strategies.get("collaborative").recommend(userId, limit));
            } catch (RuntimeException ignored) {
                recs.clear();
            }
        }
        
        if (recs.size() < limit && strategies.containsKey("content")) {
            List<Dish> contentRecs;
            try {
                contentRecs = strategies.get("content").recommend(userId, limit - recs.size());
            } catch (RuntimeException ignored) {
                contentRecs = Collections.emptyList();
            }
            Set<Long> existingIds = recs.stream().map(Dish::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (Dish d : contentRecs) {
                if (d == null || d.getId() == null || existingIds.contains(d.getId())) {
                    continue;
                }
                recs.add(d);
                existingIds.add(d.getId());
            }
        }
        
        if (recs.size() < limit && strategies.containsKey("popular")) {
            List<Dish> popRecs;
            try {
                popRecs = strategies.get("popular").recommend(userId, limit - recs.size());
            } catch (RuntimeException ignored) {
                popRecs = Collections.emptyList();
            }
            Set<Long> existingIds = recs.stream().map(Dish::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (Dish d : popRecs) {
                if (d == null || d.getId() == null || existingIds.contains(d.getId())) {
                    continue;
                }
                recs.add(d);
            }
        }
        
        return recs.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<Dish> getRecommendationsByStrategy(String strategyType, Long userId, int limit) {
        RecommendationStrategy strategy = strategies.get(strategyType);
        if (strategy == null) {
            return Collections.emptyList();
        }
        try {
            return strategy.recommend(userId, limit);
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Dish> getHealthRecommendations(Long userId, String goal, int limit) {
        // goal: "weight_loss", "muscle_gain"
        if ("weight_loss".equalsIgnoreCase(goal)) {
            // Low calories, e.g., < 500 kcal
            return dishRepository.findByCaloriesLessThan(500, PageRequest.of(0, limit));
        } else if ("muscle_gain".equalsIgnoreCase(goal)) {
            // High protein, e.g., > 20g
            return dishRepository.findByProteinGreaterThan(new BigDecimal("20"), PageRequest.of(0, limit));
        }
        return Collections.emptyList();
    }

    @Override
    public List<Dish> getDiscoveryRecommendations(Long userId, int limit) {
        log.debug("discovery推荐: userId={}, limit={}", userId, limit);
        List<Long> orderedDishIds = orderRepository.findOrderedDishIdsByUserId(userId);
        if (orderedDishIds == null || orderedDishIds.isEmpty()) {
            log.debug("discovery推荐: userId={} 无历史订单", userId);
            List<Dish> fallback = getRecommendationsByStrategy("popular", userId, limit);
            log.debug("discovery推荐: userId={} 无历史订单, popular降级返回条数={}", userId, fallback.size());
            return fallback;
        }
        log.debug("discovery推荐: userId={} 历史点过菜品数={}", userId, orderedDishIds.size());
        List<Dish> recs = dishRepository.findByIdNotIn(orderedDishIds, PageRequest.of(0, limit));
        log.debug("discovery推荐: userId={} NOT_IN推荐返回条数={}", userId, recs.size());
        if (recs.size() < limit) {
            List<Dish> fallback = getRecommendationsByStrategy("popular", userId, limit);
            Set<Long> existing = recs.stream().map(Dish::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (Dish d : fallback) {
                if (d == null || d.getId() == null || existing.contains(d.getId())) {
                    continue;
                }
                recs.add(d);
                existing.add(d.getId());
                if (recs.size() >= limit) {
                    break;
                }
            }
            log.debug("discovery推荐: userId={} 补足后返回条数={}", userId, recs.size());
        }
        return recs;
    }

    @Override
    public Map<String, List<Dish>> getComprehensiveRecommendations(Long userId) {
        Map<String, List<Dish>> result = new HashMap<>();
        
        result.put("personalized", getPersonalizedRecommendations(userId, 5));
        result.put("popular", getRecommendationsByStrategy("popular", userId, 5));
        
        // Discovery
        result.put("discovery", getDiscoveryRecommendations(userId, 5));
        
        // Context (e.g. "Lunch Special") - Placeholder
        // result.put("context", getRecommendationsByStrategy("context", userId, 5));
        
        return result;
    }
    
    @Override
    @Cacheable(value = "recommendations", key = "'today_new_' + #limit")
    public List<Dish> getTodayNewDishes(int limit) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return dishRepository.findTodayNewDishes(
                start,
                end,
                com.school.canteen.entity.Dish.DishStatus.AVAILABLE,
                PageRequest.of(0, Math.max(1, Math.min(50, limit)))
        );
    }
    
    @Override
    public List<Map<String, Object>> getPersonalizedRecommendationsWithReason(Long userId, int limit) {
        List<Dish> cf = Collections.emptyList();
        List<Dish> content = Collections.emptyList();
        List<Dish> popular = Collections.emptyList();
        
        if (strategies.containsKey("collaborative")) {
            try {
                cf = strategies.get("collaborative").recommend(userId, limit);
            } catch (RuntimeException ignored) {}
        }
        int remaining = Math.max(0, limit - cf.size());
        if (remaining > 0 && strategies.containsKey("content")) {
            try {
                content = strategies.get("content").recommend(userId, remaining);
            } catch (RuntimeException ignored) {}
        }
        remaining = Math.max(0, limit - cf.size() - content.size());
        if (remaining > 0 && strategies.containsKey("popular")) {
            try {
                popular = strategies.get("popular").recommend(userId, remaining);
            } catch (RuntimeException ignored) {}
        }
        
        Map<Long, String> reasonById = new HashMap<>();
        for (Dish d : cf) {
            if (d != null && d.getId() != null) reasonById.put(d.getId(), "根据您的评价历史推荐");
        }
        for (Dish d : content) {
            if (d != null && d.getId() != null && !reasonById.containsKey(d.getId())) reasonById.put(d.getId(), "根据您的口味偏好推荐");
        }
        for (Dish d : popular) {
            if (d != null && d.getId() != null && !reasonById.containsKey(d.getId())) reasonById.put(d.getId(), "热门菜品推荐");
        }
        
        List<Dish> merged = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Dish d : cf) {
            if (d == null || d.getId() == null || seen.contains(d.getId())) continue;
            merged.add(d); seen.add(d.getId());
            if (merged.size() >= limit) break;
        }
        if (merged.size() < limit) {
            for (Dish d : content) {
                if (d == null || d.getId() == null || seen.contains(d.getId())) continue;
                merged.add(d); seen.add(d.getId());
                if (merged.size() >= limit) break;
            }
        }
        if (merged.size() < limit) {
            for (Dish d : popular) {
                if (d == null || d.getId() == null || seen.contains(d.getId())) continue;
                merged.add(d); seen.add(d.getId());
                if (merged.size() >= limit) break;
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Dish d : merged) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("price", d.getPrice());
            m.put("imageUrl", d.getImageUrl());
            m.put("image", d.getImageUrl()); // 兼容前端不同字段名
            m.put("tasteTags", d.getTasteTags());
            m.put("category", d.getDishCategory()); // 添加大类
            m.put("subCategory", d.getSubCategory());
            m.put("status", d.getStatus() != null ? d.getStatus().name() : null);
            m.put("available", d.getStatus() == com.school.canteen.entity.Dish.DishStatus.AVAILABLE); // 明确的可用状态
            m.put("averageRating", d.getAverageRating());
            m.put("ratingCount", d.getRatingCount());
            m.put("sales", d.getRatingCount()); // 暂时用评分数模拟销量，如果有销量字段应使用getSales()
            m.put("canteenName", d.getCanteenName());
            m.put("windowName", d.getWindowName());
            m.put("windowLocation", d.getWindowLocation());
            m.put("description", d.getDescription());
            
            // 补充营养信息
            m.put("calories", d.getCalories());
            m.put("protein", d.getProtein());
            m.put("fat", d.getFat());
            m.put("carbohydrate", d.getCarbohydrate());
            
            // 补充促销信息
            m.put("isPromotion", d.getIsPromotion());
            m.put("promotionPrice", d.getPromotionPrice());
            
            m.put("reason", reasonById.get(d.getId()));
            result.add(m);
        }
        return result;
    }
}
