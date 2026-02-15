package com.school.canteen.service.strategy.impl;

import com.school.canteen.entity.Dish;
import com.school.canteen.entity.Review;
import com.school.canteen.repository.DishRepository;
import com.school.canteen.repository.ReviewRepository;
import com.school.canteen.service.strategy.RecommendationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 协同过滤推荐策略，基于用户评价相似度进行菜品推荐 */
@Component
public class CollaborativeFilteringStrategy implements RecommendationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(CollaborativeFilteringStrategy.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private static final String REC_CACHE_KEY_PREFIX = "rec:cf:user:";

    @Override
    public List<Dish> recommend(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 1. Check Cache
        String cacheKey = REC_CACHE_KEY_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Dish) {
                @SuppressWarnings("unchecked")
                List<Dish> cachedRecs = (List<Dish>) list;
                logger.debug("Returning cached CF recommendations for user {}", userId);
                return cachedRecs.stream().limit(limit).collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.warn("Redis error: {}", e.getMessage());
        }

        // 2. Calculate Recommendations (User-Based CF)
        List<Dish> recommendations = calculateUserBasedCF(userId, limit);

        // 3. Cache Result
        try {
            if (!recommendations.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, recommendations, 1, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            logger.warn("Redis error: {}", e.getMessage());
        }

        return recommendations;
    }

    private List<Dish> calculateUserBasedCF(Long targetUserId, int limit) {
        // Fetch all reviews (In production, this should be paginated or loaded incrementally)
        List<Review> allReviews = reviewRepository.findAll();
        if (allReviews == null || allReviews.isEmpty()) {
            return Collections.emptyList();
        }

        // Build User-Item Rating Matrix
        Map<Long, Map<Long, Double>> userItemRatings = new HashMap<>();
        for (Review r : allReviews) {
            if (r == null || r.getUser() == null) {
                continue;
            }
            Long userId = r.getUser().getId();
            if (userId == null || r.getItems() == null || r.getItems().isEmpty()) {
                continue;
            }
            Map<Long, Double> ratings = userItemRatings.computeIfAbsent(userId, k -> new HashMap<>());
            for (var ri : r.getItems()) {
                if (ri == null || ri.getDish() == null || ri.getDish().getId() == null || ri.getRating() == null) {
                    continue;
                }
                ratings.put(ri.getDish().getId(), ri.getRating().doubleValue());
            }
        }

        if (!userItemRatings.containsKey(targetUserId)) {
            return Collections.emptyList(); // New user, no history
        }

        Map<Long, Double> targetRatings = userItemRatings.get(targetUserId);
        Map<Long, Double> userSimilarities = new HashMap<>();

        // Calculate Similarity (Cosine Similarity)
        for (Long otherUserId : userItemRatings.keySet()) {
            if (otherUserId.equals(targetUserId)) continue;

            Map<Long, Double> otherRatings = userItemRatings.get(otherUserId);
            double similarity = calculateCosineSimilarity(targetRatings, otherRatings);
            if (similarity > 0) {
                userSimilarities.put(otherUserId, similarity);
            }
        }

        // Find Top K Similar Users
        List<Long> similarUsers = userSimilarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(10) // Top 10 neighbors
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Aggregate Recommendations
        Map<Long, Double> candidateItems = new HashMap<>();
        for (Long similarUserId : similarUsers) {
            double similarity = userSimilarities.get(similarUserId);
            Map<Long, Double> otherRatings = userItemRatings.get(similarUserId);

            for (Map.Entry<Long, Double> entry : otherRatings.entrySet()) {
                Long dishId = entry.getKey();
                Double rating = entry.getValue();

                // Only recommend items target user hasn't rated/seen
                if (!targetRatings.containsKey(dishId)) {
                    candidateItems.put(dishId, candidateItems.getOrDefault(dishId, 0.0) + similarity * rating);
                }
            }
        }

        // Sort by score
        List<Long> recommendedDishIds = candidateItems.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (recommendedDishIds.isEmpty()) {
            return Collections.emptyList();
        }

        return dishRepository.findAllById(recommendedDishIds);
    }

    private double calculateCosineSimilarity(Map<Long, Double> ratings1, Map<Long, Double> ratings2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        Set<Long> commonItems = new HashSet<>(ratings1.keySet());
        commonItems.retainAll(ratings2.keySet());

        if (commonItems.isEmpty()) return 0.0;

        for (Long itemId : ratings1.keySet()) {
            double r = ratings1.get(itemId);
            norm1 += r * r;
        }
        for (Long itemId : ratings2.keySet()) {
            double r = ratings2.get(itemId);
            norm2 += r * r;
        }
        
        for (Long itemId : commonItems) {
            dotProduct += ratings1.get(itemId) * ratings2.get(itemId);
        }

        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    @Override
    public String getStrategyType() {
        return "collaborative";
    }
}
