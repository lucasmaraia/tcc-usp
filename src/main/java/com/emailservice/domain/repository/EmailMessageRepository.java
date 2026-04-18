package com.emailservice.domain.repository;

import com.emailservice.domain.entity.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {
    List<EmailMessage> findByStatus(EmailMessage.EmailStatus status);
    List<EmailMessage> findByToEmail(String toEmail);
    List<EmailMessage> findByTemplateId(UUID templateId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EmailMessage e WHERE e.id = :id AND e.status = :status")
    boolean existsByIdAndStatus(@Param("id") UUID id, @Param("status") EmailMessage.EmailStatus status);
}
