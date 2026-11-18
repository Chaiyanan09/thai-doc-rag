package com.chaiyanan09.thairag.chunk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PgVectorTest {

    @Test
    void formatsVectorAsPgvectorLiteral() {
        assertThat(PgVector.format(new float[]{0.5f, -1f, 0.25f})).isEqualTo("[0.5,-1.0,0.25]");
    }

    @Test
    void formatsEmptyVector() {
        assertThat(PgVector.format(new float[0])).isEqualTo("[]");
    }
}
