package com.school.canteen.service.strategy.impl;

import com.school.canteen.entity.Dish;
import com.school.canteen.repository.DishRepository;
import com.school.canteen.service.strategy.RecommendationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/** 热门推荐策略，基于菜品评分和销量进行排序推荐 */
@Component
public class PopularityStrategy implements RecommendationStrategy {

    @Autowired
    private DishRepository dishRepository;

    @Override
    public List<Dish> recommend(Long userId, int limit) {
        return dishRepository.findPopularDishes(PageRequest.of(0, limit));
    }

    @Override
    public String getStrategyType() {
        return "popular";
    }
}
