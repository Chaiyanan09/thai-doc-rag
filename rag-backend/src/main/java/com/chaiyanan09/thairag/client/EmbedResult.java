package com.chaiyanan09.thairag.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** คำตอบจาก POST /embed ของ model service */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbedResult(String model, int dim, List<float[]> vectors) {
}
