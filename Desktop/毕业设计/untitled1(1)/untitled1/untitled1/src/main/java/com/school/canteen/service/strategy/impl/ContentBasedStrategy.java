package com.school.canteen.service.strategy.impl;

import com.school.canteen.entity.Dish;
import com.school.canteen.entity.User;
import com.school.canteen.entity.UserProfile;
import com.school.canteen.repository.DishRepository;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.service.strategy.RecommendationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 基于内容的推荐策略，根据用户口味偏好匹配菜品特征 */
@Component
public class ContentBasedStrategy implements RecommendationStrategy {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DishRepository dishRepository;

    @Override
    public List<Dish> recommend(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Collections.emptyList();
        }

        Set<String> dietaryTags = null;
        UserProfile profile = user.getUserProfile();
        if (profile != null && profile.getFlavorPreferences() != null) {
            String[] parts = profile.getFlavorPreferences().split(",");
            dietaryTags = java.util.Arrays.stream(parts)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } else {
            dietaryTags = user.getDietaryTags();
        }
        
        List<Dish> recommendations = new ArrayList<>();
        
        // Match dietary tags
        if (dietaryTags != null && !dietaryTags.isEmpty()) {
            for (String tag : dietaryTags) {
                List<Dish> matched = dishRepository.findByTasteTagContaining(tag);
                recommendations.addAll(matched);
            }
        }
        
        // Filter by spiciness/sweetness if we had that data on dishes normalized
        // For now, let's return unique dishes
        return recommendations.stream()
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyType() {
        return "content";
    }
}
