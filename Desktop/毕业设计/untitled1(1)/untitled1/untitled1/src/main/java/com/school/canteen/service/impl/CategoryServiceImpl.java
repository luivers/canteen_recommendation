package com.school.canteen.service.impl;

import com.school.canteen.entity.Category;
import com.school.canteen.repository.CategoryRepository;
import com.school.canteen.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 菜品分类管理服务实现类 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    @Transactional
    public Category createCategory(Category category) {
        // 如果是子分类，确保父分类存在
        if (category.getParent() != null && category.getParent().getId() != null) {
            Optional<Category> parent = categoryRepository.findById(category.getParent().getId());
            if (parent.isPresent()) {
                category.setParent(parent.get());
            } else {
                throw new RuntimeException("父分类不存在");
            }
        }
        return categoryRepository.save(category);
    }
    
    @Override
    @Transactional
    public Category updateCategory(Long id, Category category) {
        Optional<Category> existingCategory = categoryRepository.findById(id);
        if (existingCategory.isPresent()) {
            // 如果是子分类，确保父分类存在
            if (category.getParent() != null && category.getParent().getId() != null) {
                Optional<Category> parent = categoryRepository.findById(category.getParent().getId());
                if (parent.isPresent()) {
                    category.setParent(parent.get());
                } else {
                    throw new RuntimeException("父分类不存在");
                }
            }
            category.setId(id);
            return categoryRepository.save(category);
        }
        throw new RuntimeException("分类不存在");
    }
    
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
    
    @Override
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
    
    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    @Override
    public List<Category> getAllEnabledCategories() {
        return categoryRepository.findByStatus(true);
    }
    
    @Override
    public List<Category> getMainCategories() {
        // 查询所有启用的主分类（level=1）
        return categoryRepository.findByLevelAndStatus(1, true);
    }
    
    @Override
    public List<Category> getSubCategories(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }
    
    @Override
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }
    
    @Override
    @Transactional
    public void enableCategory(Long id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            category.get().setStatus(true);
            categoryRepository.save(category.get());
        } else {
            throw new RuntimeException("分类不存在");
        }
    }
    
    @Override
    @Transactional
    public void disableCategory(Long id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            category.get().setStatus(false);
            categoryRepository.save(category.get());
        } else {
            throw new RuntimeException("分类不存在");
        }
    }
}