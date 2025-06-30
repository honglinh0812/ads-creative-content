package com.fbadsautomation.repository;

import com.fbadsautomation.model.VideoGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for VideoGenerationJob entities
 */
@Repository
public interface VideoGenerationJobRepository extends JpaRepository<VideoGenerationJob, Long> {
    /**
     * Find a job by its external job ID (from Fal.ai)
     * @param externalJobId The external job ID
     * @return Optional containing the job if found
     */
    Optional<VideoGenerationJob> findByExternalJobId(String externalJobId);
    
    /**
     * Find all jobs with a specific status, ordered by creation date (newest first)
     * @param status The status to filter by
     * @return List of jobs with the specified status
     */
    List<VideoGenerationJob> findByStatusOrderByCreatedAtDesc(String status);
    List<VideoGenerationJob> findByStatusIn(List<String> statuses);
}
