package com.school.canteen.controller;

import com.school.canteen.entity.Category;
import com.school.canteen.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 菜品分类控制器 — 分类的 CRUD */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    // 分类管理API
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryService.createCategory(category));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return ResponseEntity.ok(categoryService.updateCategory(id, category));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
    
    @GetMapping("/enabled")
    public ResponseEntity<List<Category>> getAllEnabledCategories() {
        return ResponseEntity.ok(categoryService.getAllEnabledCategories());
    }
    
    // 分类层级管理API
    @GetMapping("/main")
    public ResponseEntity<List<Category>> getMainCategories() {
        return ResponseEntity.ok(categoryService.getMainCategories());
    }
    
    @GetMapping("/{id}/subcategories")
    public ResponseEntity<List<Category>> getSubCategories(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getSubCategories(id));
    }
    
    @GetMapping("/name/{name}")
    public ResponseEntity<Category> getCategoryByName(@PathVariable String name) {
        Category category = categoryService.getCategoryByName(name);
        return category != null ? ResponseEntity.ok(category) : ResponseEntity.notFound().build();
    }
    
    // 分类状态管理API
    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enableCategory(@PathVariable Long id) {
        categoryService.enableCategory(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableCategory(@PathVariable Long id) {
        categoryService.disableCategory(id);
        return ResponseEntity.ok().build();
    }
}