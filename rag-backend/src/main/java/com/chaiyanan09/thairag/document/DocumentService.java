package com.chaiyanan09.thairag.document;

import com.chaiyanan09.thairag.chunk.ChunkRepository;
import com.chaiyanan09.thairag.client.IngestResult;
import com.chaiyanan09.thairag.client.ModelServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documents;
    private final ChunkRepository chunks;
    private final ModelServiceClient modelService;
    private final TransactionTemplate tx;
    private final int embedBatchSize;

    public DocumentService(DocumentRepository documents,
                           ChunkRepository chunks,
                           ModelServiceClient modelService,
                           PlatformTransactionManager transactionManager,
                           @Value("${app.embed.batch-size:32}") int embedBatchSize) {
        this.documents = documents;
        this.chunks = chunks;
        this.modelService = modelService;
        this.tx = new TransactionTemplate(transactionManager);
        this.embedBatchSize = embedBatchSize;
    }

    /**
     * อัปโหลด PDF: บันทึกสถานะ PROCESSING -> ให้ Python อ่านและตัด chunk -> สร้าง embedding
     * -> บันทึก chunk ทั้งหมดและเปลี่ยนเป็น READY ใน transaction เดียว
     *
     * ไม่ครอบทั้งเมธอดด้วย @Transactional เพราะการเรียก model service ใช้เวลานาน
     * ไม่ควรถือ connection ของฐานข้อมูลไว้ระหว่างรอ
     */
    public DocumentResponse upload(MultipartFile file) {
        byte[] pdf = readBytes(file);
        if (!looksLikePdf(pdf)) {
            throw new IllegalArgumentException("ไฟล์นี้ไม่ใช่ PDF");
        }
        String filename = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();

        Document doc = documents.save(new Document(filename));
        try {
            IngestResult ingested = modelService.ingest(pdf);
            if (ingested.chunks() == null || ingested.chunks().isEmpty()) {
                throw new IllegalArgumentException("ไม่พบข้อความใน PDF นี้ (อาจเป็นไฟล์สแกน ต้องใช้ OCR)");
            }
            List<float[]> vectors = embedAll(ingested.chunks());

            tx.executeWithoutResult(status -> {
                chunks.insertAll(doc.getId(), ingested.chunks(), vectors);
                doc.markReady(ingested.pageCount(), ingested.chunks().size(), ingested.emptyPages());
                documents.save(doc);
            });
            log.info("Document {} ready: {} pages, {} chunks", doc.getId(), doc.getPageCount(), doc.getChunkCount());
            return DocumentResponse.from(doc);
        } catch (RuntimeException e) {
            doc.markFailed(e.getMessage());
            documents.save(doc);
            log.warn("Document {} failed: {}", doc.getId(), e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list() {
        return documents.findAllByOrderByCreatedAtDesc().stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(UUID id) {
        return documents.findById(id).map(DocumentResponse::from).orElseThrow(() -> notFound(id));
    }

    /** ลบเอกสาร chunk ของเอกสารถูกลบตามด้วย ON DELETE CASCADE ในฐานข้อมูล */
    @Transactional
    public void delete(UUID id) {
        if (!documents.existsById(id)) {
            throw notFound(id);
        }
        documents.deleteById(id);
    }

    private List<float[]> embedAll(List<IngestResult.Chunk> items) {
        List<float[]> vectors = new ArrayList<>(items.size());
        for (int from = 0; from < items.size(); from += embedBatchSize) {
            List<String> texts = items.subList(from, Math.min(from + embedBatchSize, items.size()))
                    .stream()
                    .map(IngestResult.Chunk::text)
                    .toList();
            vectors.addAll(modelService.embed(texts));
        }
        return vectors;
    }

    /** ไฟล์ PDF ต้องมี "%PDF-" อยู่ใน 1024 ไบต์แรก */
    static boolean looksLikePdf(byte[] bytes) {
        int length = Math.min(bytes.length, 1024);
        return new String(bytes, 0, length, StandardCharsets.ISO_8859_1).contains("%PDF-");
    }

    private static byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("กรุณาเลือกไฟล์ PDF");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ResponseStatusException notFound(UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบเอกสาร " + id);
    }
}
