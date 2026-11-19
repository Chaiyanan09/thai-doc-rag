package com.chaiyanan09.thairag.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CitationVerifierTest {

    // ข้อความจริงจาก sample-regulation-th.pdf
    private static final String PAGE_6 = "ข้อ 28 ปริญญาเกียรตินิยมอันดับสอง ให้แก่ผู้ที่ได้ค่าระดับคะแนนเฉลี่ยสะสมไม่ต่ำกว่า 3.25 "
            + "ข้อ 29 นักศึกษาที่เคยถูกลงโทษฐานทุจริตในการสอบ ไม่มีสิทธิ์ได้รับปริญญาเกียรตินิยม";
    private static final String PAGE_5 = "ข้อ 23 นักศึกษาที่ทุจริตในการสอบ หรือพยายามทุจริตในการสอบ จะได้รับระดับคะแนน F "
            + "ในทุกรายวิชาที่ลงทะเบียนในภาคการศึกษานั้น และให้พักการศึกษาในภาคการศึกษาปกติถัดไปหนึ่งภาค";
    private static final String PAGE_7 = "ข้อ 30 นักศึกษาต้องชำระค่าธรรมเนียมการศึกษาแบบเหมาจ่ายภาคการศึกษาปกติละ 18,000 บาท";

    private static final List<String> SOURCES = List.of(PAGE_6, PAGE_5, PAGE_7);

    @Test
    void replacesACitationThatPointsToTheWrongSource() {
        // คำตอบมาจาก S1 (ข้อ 29) แต่โมเดลอ้าง S2
        var result = CitationVerifier.verify("นักศึกษาที่เคยถูกลงโทษฐานทุจริตในการสอบ ไม่มีสิทธิ์ได้รับปริญญาเกียรตินิยม [S2]", SOURCES);

        assertThat(result.text()).endsWith("ปริญญาเกียรตินิยม [S1]");
        assertThat(result.cited()).containsExactly(1);
        assertThat(result.changes()).isEqualTo(1);
    }

    @Test
    void addsACitationWhenTheModelForgotOne() {
        var result = CitationVerifier.verify("ค่าธรรมเนียมการศึกษาแบบเหมาจ่ายภาคการศึกษาปกติละ 18,000 บาท", SOURCES);

        assertThat(result.text()).endsWith("18,000 บาท[S3]");
        assertThat(result.cited()).containsExactly(3);
    }

    @Test
    void keepsACorrectCitation() {
        String answer = "จะได้รับระดับคะแนน F ในทุกรายวิชาที่ลงทะเบียนในภาคการศึกษานั้น [S2]";

        var result = CitationVerifier.verify(answer, SOURCES);

        assertThat(result.text()).isEqualTo(answer);
        assertThat(result.changes()).isZero();
    }

    @Test
    void leavesShortOrUnsupportedSentencesAlone() {
        assertThat(CitationVerifier.verify("2.5 แต้ม [S3]", SOURCES).changes()).isZero();
        assertThat(CitationVerifier.verify("มหาวิทยาลัยมีหอพักสำหรับนักศึกษาชั้นปีที่หนึ่ง", SOURCES).changes()).isZero();
    }
}
