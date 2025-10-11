package com.fbadsautomation.repository;

import com.fbadsautomation.model.CompetitorSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompetitorSearchRepository extends JpaRepository<CompetitorSearch, Long> {
    List<CompetitorSearch> findByUserId(Long userId);
}
