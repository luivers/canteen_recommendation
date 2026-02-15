package com.school.canteen.service.impl;

import com.school.canteen.entity.Canteen;
import com.school.canteen.repository.CanteenRepository;
import com.school.canteen.service.CanteenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 食堂管理服务实现类 */
@Service
@RequiredArgsConstructor
public class CanteenServiceImpl implements CanteenService {
    
    private final CanteenRepository canteenRepository;
    
    @Override
    @Transactional
    public Canteen createCanteen(Canteen canteen) {
        return canteenRepository.save(canteen);
    }
    
    @Override
    @Transactional
    public Canteen updateCanteen(Long id, Canteen canteen) {
        Optional<Canteen> existingCanteen = canteenRepository.findById(id);
        if (existingCanteen.isPresent()) {
            canteen.setId(id);
            return canteenRepository.save(canteen);
        }
        throw new RuntimeException("食堂不存在");
    }
    
    @Override
    @Transactional
    public void deleteCanteen(Long id) {
        canteenRepository.deleteById(id);
    }
    
    @Override
    public Optional<Canteen> getCanteenById(Long id) {
        return canteenRepository.findById(id);
    }
    
    @Override
    public List<Canteen> getAllCanteens() {
        return canteenRepository.findAll();
    }
    
    @Override
    public Canteen getCanteenByName(String name) {
        return canteenRepository.findByName(name);
    }
}