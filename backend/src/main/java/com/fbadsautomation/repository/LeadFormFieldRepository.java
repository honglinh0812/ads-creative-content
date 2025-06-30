package com.fbadsautomation.repository;

import com.fbadsautomation.model.LeadFormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadFormFieldRepository extends JpaRepository<LeadFormField, Long> {
    List<LeadFormField> findByLeadFormAdId(Long leadFormAdId);
}
