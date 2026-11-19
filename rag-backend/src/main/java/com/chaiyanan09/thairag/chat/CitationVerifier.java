package com.chaiyanan09.thairag.chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ตรวจเลขอ้างอิงที่ LLM ใส่มา โดยเทียบข้อความจริง
 *
 * ผล eval พบว่าโมเดลมักตอบถูกแต่ใส่เลขอ้างอิงผิดชิ้น หรือลืมใส่ ส่วนคำตอบมักลอกถ้อยคำจากเอกสารมาเกือบตรงตัว
 * จึงตรวจได้ว่าแต่ละประโยคมาจากชิ้นไหนจริง ด้วยการนับตัวอักษร 3 ตัวติดกัน (trigram) ที่ซ้ำกับแต่ละชิ้น
 *
 * - ประโยคที่ไม่มีเลขอ้างอิง และมีชิ้นที่ตรงกันชัดเจน -> เติมเลขของชิ้นนั้น
 * - ประโยคที่อ้างชิ้นหนึ่ง แต่อีกชิ้นตรงกว่ามาก -> เปลี่ยนเป็นชิ้นที่ตรงกว่า
 * - ประโยคสั้นหรือไม่ตรงกับชิ้นไหนชัดเจน -> ไม่แตะ (ตรวจไม่ได้)
 */
public final class CitationVerifier {

    /** อย่างน้อยครึ่งหนึ่งของ trigram ในประโยคต้องอยู่ในชิ้นนั้น ถึงจะถือว่าชิ้นนั้นรองรับประโยคนี้ */
    static final double MIN_SUPPORT = 0.5;
    /** ชิ้นที่ดีที่สุดต้องตรงกว่าชิ้นที่โมเดลอ้างอย่างน้อยเท่านี้ ถึงจะเปลี่ยน */
    static final double MARGIN = 0.2;
    /** ประโยคที่สั้นกว่านี้ (ไม่นับช่องว่าง) ตรวจไม่ได้ เช่น "2.5 แต้ม" */
    static final int MIN_SEGMENT_CHARS = 15;

    private static final Pattern CITATIONS = Pattern.compile("(?:\\[S\\d+])+");
    private static final Pattern NUMBER = Pattern.compile("S(\\d+)");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    /** @param changes จำนวนประโยคที่ถูกเติมหรือเปลี่ยนเลขอ้างอิง */
    public record Result(String text, Set<Integer> cited, int changes) {
    }

    private CitationVerifier() {
    }

    /**
     * @param answer  คำตอบที่จัดเลขอ้างอิงเป็นรูปแบบ [S1] แล้ว (ผ่าน CitationParser.normalize)
     * @param sources ข้อความของชิ้นที่ส่งให้ LLM เรียงตาม S1, S2, ...
     */
    public static Result verify(String answer, List<String> sources) {
        List<Set<String>> sourceGrams = sources.stream().map(CitationVerifier::trigrams).toList();
        StringBuilder out = new StringBuilder();
        Set<Integer> cited = new LinkedHashSet<>();
        int changes = 0;

        Matcher m = CITATIONS.matcher(answer);
        int segmentStart = 0;
        boolean more = true;
        while (more) {
            more = m.find();
            String text = answer.substring(segmentStart, more ? m.start() : answer.length());
            List<Integer> given = more ? numbers(m.group()) : List.of();

            List<Integer> checked = check(text, given, sourceGrams);
            if (!checked.equals(given)) {
                changes++;
            }
            out.append(text);
            checked.forEach(n -> out.append("[S").append(n).append(']'));
            cited.addAll(checked);

            if (more) {
                segmentStart = m.end();
            }
        }
        return new Result(out.toString(), cited, changes);
    }

    private static List<Integer> check(String segment, List<Integer> given, List<Set<String>> sources) {
        if (sources.isEmpty() || SPACES.matcher(segment).replaceAll("").length() < MIN_SEGMENT_CHARS) {
            return given;
        }
        Set<String> grams = trigrams(segment);
        double[] score = new double[sources.size()];
        int best = 0;
        for (int i = 0; i < sources.size(); i++) {
            score[i] = overlap(grams, sources.get(i));
            if (score[i] > score[best]) {
                best = i;
            }
        }
        if (score[best] < MIN_SUPPORT) {
            return given;
        }
        double givenBest = given.stream()
                .filter(n -> n >= 1 && n <= sources.size())
                .mapToDouble(n -> score[n - 1])
                .max()
                .orElse(-1);
        if (given.isEmpty() || score[best] - givenBest >= MARGIN) {
            return List.of(best + 1);
        }
        return given;
    }

    /** สัดส่วนของ trigram ในประโยคที่พบในชิ้นข้อมูล (0 ถึง 1) */
    static double overlap(Set<String> segmentGrams, Set<String> sourceGrams) {
        if (segmentGrams.isEmpty()) {
            return 0;
        }
        int shared = 0;
        for (String g : segmentGrams) {
            if (sourceGrams.contains(g)) {
                shared++;
            }
        }
        return (double) shared / segmentGrams.size();
    }

    static Set<String> trigrams(String text) {
        String s = SPACES.matcher(text).replaceAll("");
        Set<String> grams = new HashSet<>();
        for (int i = 0; i + 3 <= s.length(); i++) {
            grams.add(s.substring(i, i + 3));
        }
        return grams;
    }

    private static List<Integer> numbers(String tags) {
        List<Integer> result = new ArrayList<>();
        Matcher n = NUMBER.matcher(tags);
        while (n.find()) {
            result.add(Integer.parseInt(n.group(1)));
        }
        return result;
    }
}
