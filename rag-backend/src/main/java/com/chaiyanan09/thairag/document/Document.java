package com.chaiyanan09.thairag.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** เอกสาร PDF ที่อัปโหลด 1 ไฟล์ (ตัว chunk เก็บแยกในตาราง chunks ผ่าน ChunkRepository) */
@Entity
@Table(name = "documents")
public class Document {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "empty_pages", columnDefinition = "integer[]", nullable = false)
    private List<Integer> emptyPages = new ArrayList<>();

    @Column(name = "error_message")
    private String errorMessage;

    /** SHA-256 ของไฟล์ ใช้ตรวจว่าเคยอัปโหลดไฟล์เดียวกันแล้วหรือยัง */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Document() {
        // for JPA
    }

    public Document(String filename, String contentHash) {
        this.filename = filename;
        this.contentHash = contentHash;
        this.status = DocumentStatus.PROCESSING;
        this.createdAt = Instant.now();
    }

    public void markReady(int pageCount, int chunkCount, List<Integer> emptyPages) {
        this.status = DocumentStatus.READY;
        this.pageCount = pageCount;
        this.chunkCount = chunkCount;
        this.emptyPages = emptyPages == null ? new ArrayList<>() : new ArrayList<>(emptyPages);
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        String text = (message == null || message.isBlank()) ? "เกิดข้อผิดพลาดที่ไม่ทราบสาเหตุ" : message;
        this.status = DocumentStatus.FAILED;
        this.errorMessage = text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }

    public UUID getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public List<Integer> getEmptyPages() {
        return emptyPages;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
