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
}
