package com.emailservice.domain.repository;

import com.emailservice.domain.entity.EmailMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {
    Optional<EmailMessage> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByIdAndStatus(UUID id, EmailMessage.EmailStatus status);

    @Query("""
            select m from EmailMessage m
            where m.userId = :userId
              and (:status is null or m.status = :status)
              and (:toEmail is null or lower(m.toEmail) like lower(concat('%', :toEmail, '%')))
            """)
    Page<EmailMessage> search(@Param("userId") UUID userId,
                              @Param("status") EmailMessage.EmailStatus status,
                              @Param("toEmail") String toEmail,
                              Pageable pageable);
    List<EmailMessage> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);
    Page<EmailMessage> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<EmailMessage> findByUserIdAndStatusAndCreatedAtBetween(
            UUID userId, EmailMessage.EmailStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
