package com.chaiyanan09.thairag.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * เรียก Python model service (rag-model-service) ผ่าน HTTP
 * แบบเดียวกับ MlClient ใน ToothSeg แต่ใช้ RestClient ของ Spring แปลง JSON ให้อัตโนมัติ
 */
@Component
public class ModelServiceClient {

    private final RestClient rest;

    public ModelServiceClient(@Value("${app.model-service.base-url}") String baseUrl,
                              @Value("${app.model-service.api-key:}") String apiKey,
                              @Value("${app.model-service.timeout-seconds:300}") long timeoutSeconds) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        // PDF ยาวๆ + bge-m3 บน CPU อาจใช้เวลาหลายนาที
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory);
        if (!apiKey.isBlank()) {
            builder.defaultHeader("X-API-KEY", apiKey);
        }
        this.rest = builder.build();
    }

    /** ส่ง PDF ไปอ่านและตัด chunk */
    public IngestResult ingest(byte[] pdf) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_PDF);
        // ชื่อไฟล์จริงเก็บไว้ที่ backend แล้ว ส่งชื่อภาษาอังกฤษไปเพื่อเลี่ยงปัญหา encoding ใน multipart
        partHeaders.setContentDispositionFormData("file", "document.pdf");

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new HttpEntity<>(pdf, partHeaders));

        return call("/ingest", () -> rest.post()
                .uri("/ingest")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::throwError)
                .body(IngestResult.class));
    }

    /** แปลงข้อความเป็น vector 1024 มิติ (1 vector ต่อ 1 ข้อความ ตามลำดับเดิม) */
    public List<float[]> embed(List<String> texts) {
        EmbedResult result = call("/embed", () -> rest.post()
                .uri("/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbedRequest(texts))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::throwError)
                .body(EmbedResult.class));

        if (result.vectors() == null || result.vectors().size() != texts.size()) {
            throw new ModelServiceException("จำนวน vector ที่ได้ไม่ตรงกับจำนวนข้อความ");
        }
        return result.vectors();
    }

    public boolean isHealthy() {
        try {
            rest.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    private <T> T call(String path, Supplier<T> request) {
        try {
            T body = request.get();
            if (body == null) {
                throw new ModelServiceException("model service ตอบกลับว่างจาก " + path);
            }
            return body;
        } catch (ResourceAccessException e) {
            throw new ModelServiceException(
                    "ติดต่อ model service ไม่ได้ (" + path + ") เปิด uvicorn ที่พอร์ต 8001 แล้วหรือยัง", e);
        }
    }

    private void throwError(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        throw new ModelServiceException(
                "model service ตอบ " + response.getStatusCode().value() + " ที่ " + request.getURI().getPath() + ": " + body);
    }
}
