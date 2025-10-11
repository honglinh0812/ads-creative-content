package com.fbadsautomation.repository;

import com.fbadsautomation.model.AsyncJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsyncJobStatusRepository extends JpaRepository<AsyncJobStatus, Long> {

    Optional<AsyncJobStatus> findByJobId(String jobId);

    List<AsyncJobStatus> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AsyncJobStatus> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AsyncJobStatus.Status status);

    @Query("SELECT a FROM AsyncJobStatus a WHERE a.userId = :userId AND a.status IN :statuses ORDER BY a.createdAt DESC")
    List<AsyncJobStatus> findByUserIdAndStatusInOrderByCreatedAtDesc(@Param("userId") Long userId,
                                                                     @Param("statuses") List<AsyncJobStatus.Status> statuses);

    @Query("SELECT a FROM AsyncJobStatus a WHERE a.expiresAt < :now")
    List<AsyncJobStatus> findExpiredJobs(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM AsyncJobStatus a WHERE a.expiresAt < :now")
    int deleteExpiredJobs(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE AsyncJobStatus a SET a.status = :newStatus WHERE a.status = :oldStatus AND a.expiresAt < :now")
    int markExpiredJobs(@Param("oldStatus") AsyncJobStatus.Status oldStatus,
                       @Param("newStatus") AsyncJobStatus.Status newStatus,
                       @Param("now") LocalDateTime now);

    Long countByUserIdAndStatus(Long userId, AsyncJobStatus.Status status);

    @Query("SELECT COUNT(a) FROM AsyncJobStatus a WHERE a.userId = :userId AND a.status IN :statuses")
    Long countByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<AsyncJobStatus.Status> statuses);
}