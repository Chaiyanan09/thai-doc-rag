package com.chaiyanan09.thairag.chat;

/**
 * @param sourceFormat "plain" หรือ "tagged" (ดู PromptBuilder.SourceFormat)
 */
public record ChatSettings(String model, int topK, double minScore, String sourceFormat, String promptVersion,
                           boolean verifyCitations) {
}
