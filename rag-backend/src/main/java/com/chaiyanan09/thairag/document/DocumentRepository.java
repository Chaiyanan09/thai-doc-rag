package com.chaiyanan09.thairag.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findAllByOrderByCreatedAtDesc();

    Optional<Document> findFirstByContentHashAndStatusOrderByCreatedAtDesc(String contentHash, DocumentStatus status);
}
