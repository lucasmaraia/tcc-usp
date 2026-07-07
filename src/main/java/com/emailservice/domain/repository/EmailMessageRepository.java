package com.emailservice.domain.repository;

import com.emailservice.domain.entity.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {
    Optional<EmailMessage> findByIdAndUserId(UUID id, UUID userId);
    List<EmailMessage> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<EmailMessage> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, EmailMessage.EmailStatus status);
    boolean existsByIdAndStatus(UUID id, EmailMessage.EmailStatus status);
}
