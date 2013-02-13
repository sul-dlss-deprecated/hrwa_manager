package edu.columbia.ldpd.hrwa.marc.z3950;

import static edu.columbia.ldpd.hrwa.marc.z3950.OIDValue.decodeBase128;
import static edu.columbia.ldpd.hrwa.marc.z3950.OIDValue.encodeBase128;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class OIDTest {

    @Test
    public void test() {
        assertArrayEquals(new long[]{129,0},encodeBase128(128));
        assertArrayEquals(new long[]{130,129,3},encodeBase128(32899));
        assertEquals(128, decodeBase128(new long[]{129,0}));
        assertEquals(32899, decodeBase128(new long[]{130,129,3}));
    }

}
