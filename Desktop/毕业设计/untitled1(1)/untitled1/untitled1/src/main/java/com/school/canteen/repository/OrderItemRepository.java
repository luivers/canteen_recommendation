package com.school.canteen.repository;

import com.school.canteen.entity.Order;
import com.school.canteen.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/** 订单子项数据访问层 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            SELECT oi
            FROM OrderItem oi
            JOIN FETCH oi.dish d
            JOIN oi.order o
            WHERE o.user.id = :userId
              AND o.status IN :statuses
              AND oi.createTime BETWEEN :start AND :end
            """)
    List<OrderItem> findByUserIdAndCreateTimeBetweenFetchDish(
            @Param("userId") Long userId,
            @Param("statuses") List<Order.OrderStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}

