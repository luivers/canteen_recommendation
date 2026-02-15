package com.school.canteen.repository;

import com.school.canteen.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 菜品分类数据访问层 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 查询所有启用的分类
    List<Category> findByStatus(Boolean status);
    
    // 根据级别查询分类
    List<Category> findByLevel(Integer level);
    
    // 根据父分类查询子分类
    List<Category> findByParentId(Long parentId);
    
    // 查询所有启用的主分类
    List<Category> findByLevelAndStatus(Integer level, Boolean status);
    
    // 根据名称查询分类
    Category findByName(String name);
}