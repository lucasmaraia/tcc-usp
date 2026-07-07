package com.emailservice.domain.repository;

import com.emailservice.domain.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    Optional<EmailTemplate> findByIdAndUserId(UUID id, UUID userId);
    Optional<EmailTemplate> findByNameAndUserId(String name, UUID userId);
    List<EmailTemplate> findByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByNameAndUserId(String name, UUID userId);
}
