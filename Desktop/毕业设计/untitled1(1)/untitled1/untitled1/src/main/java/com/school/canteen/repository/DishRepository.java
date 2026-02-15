package com.school.canteen.repository;

import com.school.canteen.entity.Category;
import com.school.canteen.entity.Dish;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 菜品数据访问层 */
@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {
    
    // 根据分类实体查询菜品
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.category = :category AND d.status != 'DELETED'")
    List<Dish> findByCategory(@Param("category") Category category);
    
    // 根据分类ID查询菜品
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.category.id = :categoryId AND d.status != 'DELETED'")
    List<Dish> findByCategoryId(@Param("categoryId") Long categoryId);
    
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.status = :status")
    List<Dish> findByStatus(@Param("status") Dish.DishStatus status);
    
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE d.isPromotion = true")
    List<Dish> findByIsPromotionTrue();
    
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE d.isPromotion = true AND d.promotionStart <= CURRENT_TIMESTAMP AND d.promotionEnd >= CURRENT_TIMESTAMP")
    List<Dish> findActivePromotionDishes();
    
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE d.isPromotion = true AND d.promotionStart <= CURRENT_TIMESTAMP AND d.promotionEnd >= CURRENT_TIMESTAMP ORDER BY d.promotionPrice ASC")
    List<Dish> findActivePromotionDishesOrderByPriceAsc();
    
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE d.isPromotion = true AND d.promotionStart <= CURRENT_TIMESTAMP AND d.promotionEnd >= CURRENT_TIMESTAMP ORDER BY d.averageRating DESC")
    List<Dish> findActivePromotionDishesOrderByRatingDesc();
    
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE d.averageRating >= :minRating ORDER BY d.averageRating DESC")
    List<Dish> findTopRatedDishes(@Param("minRating") Double minRating, Pageable pageable);
    
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE (d.name LIKE %:keyword% OR d.description LIKE %:keyword%) AND d.status != 'DELETED'")
    List<Dish> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT d FROM Dish d ORDER BY d.ratingCount DESC, d.createTime DESC")
    List<Dish> findPopularDishes(Pageable pageable);
    
    // 获取所有菜品，用于测试
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE d.status != 'DELETED'")
    List<Dish> findAllDishes();
    
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE d.tasteTags LIKE %:tag% AND d.status != 'DELETED'")
    List<Dish> findByTasteTag(@Param("tag") String tag);
    
    // 根据菜品类型查询菜品
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE d.dishCategory = :dishCategory AND d.status != 'DELETED'")
    List<Dish> findByDishCategory(@Param("dishCategory") Dish.DishCategory dishCategory);
    
    // 今日上新：按当天创建时间范围筛选，并按创建时间倒序
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category LEFT JOIN FETCH d.tasteTags WHERE d.createTime >= :start AND d.createTime < :end AND d.status = :status ORDER BY d.createTime DESC")
    List<Dish> findTodayNewDishes(@Param("start") java.time.LocalDateTime start,
                                  @Param("end") java.time.LocalDateTime end,
                                  @Param("status") Dish.DishStatus status,
                                  Pageable pageable);
    
    // 根据窗口ID获取菜品
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.windowId = :windowId AND d.status != 'DELETED'")
    List<Dish> findByWindowId(@Param("windowId") Long windowId);

    // 获取所有去重的细分分类 (sub_category)
    @Query("SELECT DISTINCT d.subCategory FROM Dish d WHERE d.subCategory IS NOT NULL AND d.subCategory != '' AND d.status != 'DELETED'")
    List<String> findDistinctSubCategories();
    
    // 重写findAll方法，使用LEFT JOIN FETCH，确保即使没有分类也能返回菜品
    // 根据ID列表排除菜品 (用于发现新菜品)
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.id NOT IN :ids")
    List<Dish> findByIdNotIn(@Param("ids") List<Long> ids, Pageable pageable);

    // 根据卡路里筛选 (用于减肥目标)
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.calories <= :maxCalories")
    List<Dish> findByCaloriesLessThan(@Param("maxCalories") Integer maxCalories, Pageable pageable);

    // 根据蛋白质筛选 (用于增肌目标)
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.protein >= :minProtein")
    List<Dish> findByProteinGreaterThan(@Param("minProtein") java.math.BigDecimal minProtein, Pageable pageable);

    // 根据标签模糊查询 (用于口味匹配)
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.tasteTags LIKE %:tag%")
    List<Dish> findByTasteTagContaining(@Param("tag") String tag);

    @Override
    @org.springframework.lang.NonNull
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.status != 'DELETED'")
    List<Dish> findAll();

    // 每日库存重置：将所有状态为AVAILABLE且dailyLimit>0的菜品库存重置为dailyLimit
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Dish d SET d.stock = d.dailyLimit WHERE d.dailyLimit IS NOT NULL AND d.dailyLimit > 0 AND d.status = :status")
    void resetDailyInventory(@Param("status") Dish.DishStatus status);
}
