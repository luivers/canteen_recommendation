package com.school.canteen.service.impl;

import com.school.canteen.entity.Combo;
import com.school.canteen.repository.ComboRepository;
import com.school.canteen.service.ComboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 套餐管理服务实现类 */
@Service
public class ComboServiceImpl implements ComboService {

    @Autowired
    private ComboRepository comboRepository;

    @Override
    public List<Combo> getAllCombos() {
        return comboRepository.findAll();
    }

    @Override
    public Combo getComboById(Long id) {
        return comboRepository.findById(id).orElse(null);
    }

    @Override
    public List<Combo> getCombosByPromotionId(Long promotionId) {
        return comboRepository.findByPromotionId(promotionId);
    }

    @Transactional
    @Override
    public Combo createCombo(Combo combo) {
        // 设置默认值
        if (combo.getStatus() == null) {
            combo.setStatus("active");
        }
        return comboRepository.save(combo);
    }

    @Transactional
    @Override
    public Combo updateCombo(Long id, Combo combo) {
        Combo existingCombo = comboRepository.findById(id).orElse(null);
        if (existingCombo == null) {
            return null;
        }

        // 更新属性，排除ID和关联关系
        BeanUtils.copyProperties(combo, existingCombo, "id", "promotion", "dishes");
        return comboRepository.save(existingCombo);
    }

    @Transactional
    @Override
    public void deleteCombo(Long id) {
        comboRepository.deleteById(id);
    }

    @Transactional
    @Override
    public Combo toggleComboStatus(Long id) {
        Combo combo = comboRepository.findById(id).orElse(null);
        if (combo == null) {
            return null;
        }

        // 切换状态
        if (combo.getStatus().equals("active")) {
            combo.setStatus("disabled");
        } else {
            combo.setStatus("active");
        }

        return comboRepository.save(combo);
    }

    @Override
    public List<Combo> getActiveCombos() {
        return comboRepository.findByStatus("active");
    }

    @Override
    public List<Combo> getActiveCombosByPromotionId(Long promotionId) {
        return comboRepository.findByPromotionIdAndStatus(promotionId, "active");
    }
}