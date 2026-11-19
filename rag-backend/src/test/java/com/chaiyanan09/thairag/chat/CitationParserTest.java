package com.chaiyanan09.thairag.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CitationParserTest {

    @Test
    void readsSCodesInOrderOfAppearance() {
        var result = CitationParser.normalize("ลงทะเบียนได้ไม่เกิน 22 หน่วยกิต [S2] ภาคฤดูร้อนไม่เกิน 9 หน่วยกิต [S1][S2]", 5);

        assertThat(result.cited()).containsExactly(2, 1);
        assertThat(result.text()).isEqualTo("ลงทะเบียนได้ไม่เกิน 22 หน่วยกิต [S2] ภาคฤดูร้อนไม่เกิน 9 หน่วยกิต [S1][S2]");
    }

    @Test
    void splitsCommaSeparatedCodes() {
        var result = CitationParser.normalize("ต้องยื่นต่อคณบดี [S1, S3]", 5);

        assertThat(result.cited()).containsExactly(1, 3);
        assertThat(result.text()).isEqualTo("ต้องยื่นต่อคณบดี [S1][S3]");
    }

    @Test
    void clauseNumberIsNotACitationWhenSCodesAreUsed() {
        // [14] หมายถึงข้อ 14 ของระเบียบ ไม่ใช่แหล่งข้อมูลที่ 14
        var result = CitationParser.normalize("ตามข้อ [14] ต้องยื่นต่อคณบดี [S1]", 5);

        assertThat(result.cited()).containsExactly(1);
        assertThat(result.text()).isEqualTo("ตามข้อ [14] ต้องยื่นต่อคณบดี [S1]");
    }

    @Test
    void acceptsPlainNumbersOnlyWhenModelIgnoredTheFormat() {
        var result = CitationParser.normalize("ต้องยื่นต่อคณบดี [2] ภายในสองสัปดาห์ [14]", 5);

        assertThat(result.cited()).containsExactly(2);
        assertThat(result.text()).isEqualTo("ต้องยื่นต่อคณบดี [S2] ภายในสองสัปดาห์ [14]");
    }

    @Test
    void dropsCodesWithoutAMatchingSource() {
        var result = CitationParser.normalize("ดูรายละเอียด [S9] เพิ่มเติม", 5);

        assertThat(result.cited()).isEmpty();
        assertThat(result.text()).isEqualTo("ดูรายละเอียด เพิ่มเติม");
    }

    @Test
    void removesThinkingBlock() {
        assertThat(CitationParser.clean("<think>คิดก่อน\nตอบ</think>\n คำตอบจริง [S1] ")).isEqualTo("คำตอบจริง [S1]");
        assertThat(CitationParser.clean(null)).isEmpty();
    }
}
