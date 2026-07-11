package com.emailservice.domain.repository;

import com.emailservice.domain.entity.EmailTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    Optional<EmailTemplate> findByIdAndUserId(UUID id, UUID userId);
    Optional<EmailTemplate> findByNameAndUserId(String name, UUID userId);
    boolean existsByNameAndUserId(String name, UUID userId);

    @Query("""
            select t from EmailTemplate t
            where t.user.id = :userId
              and (:name is null or lower(t.name) like lower(concat('%', :name, '%')))
            """)
    Page<EmailTemplate> searchByUserId(@Param("userId") UUID userId,
                                       @Param("name") String name,
                                       Pageable pageable);
}
