package com.chaiyanan09.thairag.document;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentServiceTest {

    @Test
    void acceptsFilesThatStartWithPdfHeader() {
        assertThat(DocumentService.looksLikePdf("%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII))).isTrue();
    }

    @Test
    void rejectsOtherFiles() {
        assertThat(DocumentService.looksLikePdf("hello".getBytes(StandardCharsets.US_ASCII))).isFalse();
        assertThat(DocumentService.looksLikePdf(new byte[0])).isFalse();
    }

    @Test
    void sha256GivesTheSameHashForTheSameBytes() {
        byte[] pdf = "%PDF-1.7\nhello".getBytes(StandardCharsets.US_ASCII);
        assertThat(DocumentService.sha256(pdf))
                .hasSize(64)
                .isEqualTo(DocumentService.sha256(pdf.clone()))
                .isNotEqualTo(DocumentService.sha256("%PDF-1.7\nother".getBytes(StandardCharsets.US_ASCII)));
        // ค่ามาตรฐานของข้อความว่าง
        assertThat(DocumentService.sha256(new byte[0]))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}
