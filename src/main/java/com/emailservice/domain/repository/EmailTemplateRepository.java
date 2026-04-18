package com.emailservice.domain.repository;

import com.emailservice.domain.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    Optional<EmailTemplate> findByName(String name);
    List<EmailTemplate> findByUserId(UUID userId);
    boolean existsByName(String name);
    boolean existsByNameAndUserId(String name, UUID userId);
}
