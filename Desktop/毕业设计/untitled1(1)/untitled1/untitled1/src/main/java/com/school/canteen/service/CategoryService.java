package com.school.canteen.service;

import com.school.canteen.entity.Category;

import java.util.List;
import java.util.Optional;

/** 菜品分类管理服务接口 */
public interface CategoryService {
    // 分类管理
    Category createCategory(Category category);
    Category updateCategory(Long id, Category category);
    void deleteCategory(Long id);
    Optional<Category> getCategoryById(Long id);
    List<Category> getAllCategories();
    List<Category> getAllEnabledCategories();
    
    // 分类层级管理
    List<Category> getMainCategories();
    List<Category> getSubCategories(Long parentId);
    Category getCategoryByName(String name);
    
    // 分类状态管理
    void enableCategory(Long id);
    void disableCategory(Long id);
}