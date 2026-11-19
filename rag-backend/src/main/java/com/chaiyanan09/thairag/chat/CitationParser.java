package com.chaiyanan09.thairag.chat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * อ่านเลขอ้างอิงจากคำตอบของ LLM แล้วจัดให้อยู่รูปแบบเดียว [S1][S3] เพื่อให้หน้าเว็บแสดงเป็นปุ่ม
 *
 * ใช้รหัส S1, S2, ... แทนเลขเปล่า เพราะเอกสารระเบียบมีเลข "ข้อ 14" อยู่แล้ว
 * ถ้าให้ LLM อ้างอิงด้วย [1] มันอาจเขียน [14] โดยหมายถึงข้อ 14 แทนที่จะเป็นแหล่งข้อมูล
 */
public final class CitationParser {

    /** ข้อความในวงเล็บเหลี่ยมสั้นๆ เช่น [S1], [S1, S3], [2] */
    private static final Pattern BRACKET = Pattern.compile("\\[([^\\[\\]]{1,30})]");
    private static final Pattern S_TOKEN = Pattern.compile("(?i)s(\\d+)");
    private static final Pattern NUMBER_TOKEN = Pattern.compile("\\d+");
    private static final Pattern THINKING = Pattern.compile("(?s)<think>.*?</think>");

    /** @param text  คำตอบที่เลขอ้างอิงถูกจัดเป็น [S1][S2] แล้ว
     *  @param cited เลขแหล่งข้อมูลที่ถูกอ้างถึง ตามลำดับที่ปรากฏ */
    public record Result(String text, Set<Integer> cited) {
    }

    private CitationParser() {
    }

    /**
     * - [S1], [S1, S3] คือเลขอ้างอิง
     * - [2] ตัวเลขเปล่า นับเป็นเลขอ้างอิงเฉพาะเมื่อคำตอบไม่มีรูปแบบ [S..] เลย (เผื่อโมเดลไม่ทำตามรูปแบบ)
     * - เลขที่ไม่มีแหล่งข้อมูลรองรับ (เช่น [S9] เมื่อส่งไป 5 ชิ้น) ถูกตัดทิ้ง
     * - วงเล็บอื่นๆ ไม่ถูกแตะ
     */
    public static Result normalize(String answer, int sourceCount) {
        boolean hasSCitation = false;
        Matcher scan = BRACKET.matcher(answer);
        while (scan.find()) {
            if (kind(scan.group(1)) == Kind.S) {
                hasSCitation = true;
                break;
            }
        }

        Set<Integer> cited = new LinkedHashSet<>();
        StringBuilder out = new StringBuilder();
        Matcher m = BRACKET.matcher(answer);
        while (m.find()) {
            Kind kind = kind(m.group(1));
            String replacement = m.group();
            if (kind == Kind.S || (kind == Kind.NUMBER && !hasSCitation)) {
                List<Integer> valid = new ArrayList<>();
                for (int n : numbers(m.group(1))) {
                    if (n >= 1 && n <= sourceCount) {
                        valid.add(n);
                    }
                }
                if (!valid.isEmpty()) {
                    StringBuilder tags = new StringBuilder();
                    valid.forEach(n -> tags.append("[S").append(n).append(']'));
                    replacement = tags.toString();
                    cited.addAll(valid);
                } else if (kind == Kind.S) {
                    replacement = "";  // อ้างแหล่งที่ไม่มีอยู่จริง
                }
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return new Result(out.toString().replaceAll(" {2,}", " ").strip(), cited);
    }

    /** ลบส่วน "คิดในใจ" (<think>...</think>) ที่บางโมเดลส่งมา แล้วตัดช่องว่างหัวท้าย */
    public static String clean(String raw) {
        return raw == null ? "" : THINKING.matcher(raw).replaceAll("").strip();
    }

    private enum Kind { S, NUMBER, OTHER }

    private static Kind kind(String inside) {
        String[] tokens = inside.strip().split("[,\\s]+");
        boolean allS = true;
        boolean allNumbers = true;
        for (String token : tokens) {
            allS &= S_TOKEN.matcher(token).matches();
            allNumbers &= NUMBER_TOKEN.matcher(token).matches();
        }
        return allS ? Kind.S : allNumbers ? Kind.NUMBER : Kind.OTHER;
    }

    private static List<Integer> numbers(String inside) {
        List<Integer> result = new ArrayList<>();
        for (String token : inside.strip().split("[,\\s]+")) {
            result.add(Integer.parseInt(token.replaceAll("(?i)s", "")));
        }
        return result;
    }
}
