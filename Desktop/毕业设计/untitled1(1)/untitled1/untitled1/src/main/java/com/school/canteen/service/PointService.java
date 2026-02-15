package com.school.canteen.service;

import com.school.canteen.entity.PointLog;
import com.school.canteen.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 积分管理服务接口 */
public interface PointService {
    
    /**
     * 记录积分获取
     */
    void logEarn(User user, Integer points, PointLog.PointSource source, String description);
    
    /**
     * 记录积分消费
     */
    void logSpend(User user, Integer points, PointLog.PointSource source, String description);
    
    /**
     * 获取用户的积分历史
     */
    Page<PointLog> getUserPointHistory(Long userId, Pageable pageable);
}
