package com.chaiyanan09.thairag.chat;

import com.chaiyanan09.thairag.chunk.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Test
    void numbersEachChunkWithFileAndPage() {
        List<SearchHit> hits = List.of(
                new SearchHit(10, UUID.randomUUID(), "rules.pdf", 3, "ข้อ 16 ค่ารักษาสภาพ 1,000 บาท", 0.61),
                new SearchHit(11, UUID.randomUUID(), "rules.pdf", 5, "ข้อ 23 ทุจริตในการสอบ", 0.48));

        String prompt = PromptBuilder.userPrompt("ลาพักต้องจ่ายเงินไหม", hits);

        assertThat(prompt)
                .contains("[S1] ไฟล์: rules.pdf หน้า 3\nข้อ 16 ค่ารักษาสภาพ 1,000 บาท")
                .contains("[S2] ไฟล์: rules.pdf หน้า 5\nข้อ 23 ทุจริตในการสอบ")
                .endsWith("คำถาม: ลาพักต้องจ่ายเงินไหม");
    }

    @Test
    void taggedFormatWrapsEachChunkInASourceTag() {
        List<SearchHit> hits = List.of(new SearchHit(10, UUID.randomUUID(), "rules.pdf", 3, "ข้อ 16 ค่ารักษาสภาพ", 0.61));

        String prompt = PromptBuilder.userPrompt("ลาพักต้องจ่ายเงินไหม", hits, PromptBuilder.SourceFormat.TAGGED);

        assertThat(prompt).contains("<source id=\"S1\" file=\"rules.pdf\" page=\"3\">\nข้อ 16 ค่ารักษาสภาพ\n</source>");
    }

    @Test
    void systemPromptTellsTheModelWhatToSayWhenNothingIsFound() {
        assertThat(PromptBuilder.SYSTEM_PROMPT).contains(PromptBuilder.NOT_FOUND);
    }

    @Test
    void notFoundOnlyWhenTheAnswerSaysSoWithoutCitingAnything() {
        assertThat(PromptBuilder.isFound(PromptBuilder.NOT_FOUND, Set.of())).isFalse();
        // โมเดลตอบว่าไม่พบ แต่แปะรหัสอ้างอิงมาทุกชิ้น = ยังนับว่าไม่พบ
        assertThat(PromptBuilder.isFound(PromptBuilder.NOT_FOUND + " [S1][S2][S3]", Set.of(1, 2, 3))).isFalse();
        assertThat(PromptBuilder.isFound("**" + PromptBuilder.NOT_FOUND + "** [S1]", Set.of(1))).isFalse();
        // ตอบได้บางส่วน แล้วบอกว่าส่วนที่เหลือไม่มี = ยังนับว่าหาเจอ
        assertThat(PromptBuilder.isFound("ต้องยื่นต่อคณบดี [S1] ส่วนค่าธรรมเนียม" + PromptBuilder.NOT_FOUND, Set.of(1))).isTrue();
        assertThat(PromptBuilder.isFound("ต้องยื่นต่อคณบดี", Set.of())).isTrue();
    }
}
