package com.school.canteen.repository;

import com.school.canteen.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 消息通知数据访问层 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdAndDeletedFalseOrderByCreateTimeDesc(Long userId, Pageable pageable);
    Page<Notification> findByUserIdAndDeletedFalseAndTypeOrderByCreateTimeDesc(Long userId, Notification.NotificationType type, Pageable pageable);
    Page<Notification> findByUserIdAndDeletedFalseAndBizTypeAndTitleOrderByCreateTimeDesc(Long userId, Notification.BizType bizType, String title, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.deleted = true WHERE n.userId = :userId AND n.isRead = true AND n.deleted = false")
    void markReadAsDeleted(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.deleted = false AND NOT (n.bizType = :bizType AND n.title = :title) ORDER BY n.createTime DESC")
    Page<Notification> findByUserIdAndDeletedFalseAndExcludeBizTypeAndTitle(@Param("userId") Long userId,
                                                                            @Param("bizType") Notification.BizType bizType,
                                                                            @Param("title") String title,
                                                                            Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.deleted = false AND n.type = :type AND NOT (n.bizType = :bizType AND n.title = :title) ORDER BY n.createTime DESC")
    Page<Notification> findByUserIdAndDeletedFalseAndTypeAndExcludeBizTypeAndTitle(@Param("userId") Long userId,
                                                                                   @Param("type") Notification.NotificationType type,
                                                                                   @Param("bizType") Notification.BizType bizType,
                                                                                   @Param("title") String title,
                                                                                   Pageable pageable);

    long countByUserIdAndDeletedFalseAndIsReadFalse(Long userId);
    long countByUserIdAndDeletedFalseAndIsReadFalseAndBizTypeAndTitle(Long userId, Notification.BizType bizType, String title);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.deleted = false AND n.isRead = false AND NOT (n.bizType = :bizType AND n.title = :title)")
    long countByUserIdAndDeletedFalseAndIsReadFalseAndExcludeBizTypeAndTitle(@Param("userId") Long userId,
                                                                             @Param("bizType") Notification.BizType bizType,
                                                                             @Param("title") String title);

    List<Notification> findByUserIdAndDeletedFalseAndIsReadFalse(Long userId);
    List<Notification> findByUserIdAndDeletedFalseAndIsReadFalseAndBizTypeAndTitle(Long userId, Notification.BizType bizType, String title);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.deleted = false AND n.isRead = false AND NOT (n.bizType = :bizType AND n.title = :title)")
    List<Notification> findByUserIdAndDeletedFalseAndIsReadFalseAndExcludeBizTypeAndTitle(@Param("userId") Long userId,
                                                                                          @Param("bizType") Notification.BizType bizType,
                                                                                          @Param("title") String title);

    java.util.Optional<Notification> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    void deleteByBizTypeAndBizId(Notification.BizType bizType, Long bizId);
}
