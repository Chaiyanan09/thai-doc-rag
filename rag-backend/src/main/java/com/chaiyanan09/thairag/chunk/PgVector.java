package com.chaiyanan09.thairag.chunk;

/** แปลง float[] เป็นข้อความรูปแบบ pgvector เช่น "[0.1,-0.2,0.3]" แล้วใช้ CAST(? AS vector) ใน SQL */
public final class PgVector {

    private PgVector() {
    }

    public static String format(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 12).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
