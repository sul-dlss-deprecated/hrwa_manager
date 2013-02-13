package edu.columbia.ldpd.hrwa.marc.z3950;

import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.pow;

import java.util.Arrays;

public class OIDValue {
    private final long[] encoded;
    private final long[] srcValues;
    public OIDValue(long[] values){
        int len = 1;
        for (int i=2;i<values.length;i++){
           len += ((int)floor(log(values[i])/log(128)) + 1);
        }

        long [] encoded = new long[len];
        encoded[0] = 40 * values[0] + values[1];
        int offset = 1;
        if (values.length > 2) {
            for (int i=2;i<values.length;i++){
                long[] enc = encodeBase128(values[i]);
                for (int j=0;j<enc.length;j++){
                    encoded[j+offset] = enc[j];
                }
                offset += enc.length;
            }
        }
        this.encoded = encoded;
        this.srcValues = values;
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof OIDValue)) return false;
        OIDValue v = (OIDValue)o;
        return Arrays.equals(this.encoded, v.encoded) && Arrays.equals(this.srcValues, v.srcValues);
    }

    static long[] encodeBase128(long val){
        if (val == 0) return new long[]{0};
        int card = (int)floor(log(val)/log(128));
        int len = card + 1;
        long[] result = new long[len];
        for (int i=0;i<result.length;i++){
            long base = (long)pow(128, card - i);
            result[i] = (val / base) | 0x80;
            val = val % base;
        }
        if (len > 0){
            // the delimiter is the byte with no high bit set
            result[len -1] = 0x7F & result[len - 1];
        }
        return result;
    }
    static long decodeBase128(long[]encoded) {
        long result = 0;
        long mult = (long)pow(128,encoded.length - 1);
        for (long l:encoded){
            result += (l & 0x7F)*mult;
            mult = mult/128;
        }
        return result;
    }
    public static void main(String[] args){
        assert(Arrays.equals(new long[]{2,1,3},encodeBase128(16515)));
    }
}
