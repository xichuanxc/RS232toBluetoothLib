package com.smartpos.mrs2btlib;

/**
 * Created by XC on 2018/05/28
 */

public class CommonUtils {
    //Calculate LRC checkvalue
    public static byte calcLRC(byte[] buf, int start, int end) {
        if(start > end)
            return 0;

        if(start >= buf.length || end > buf.length) {
            return 0;
        }

        byte lrcValue = buf[start];

        for(int var3 = start + 1; var3 < end; ++var3) {
            lrcValue ^= buf[var3];
        }

        return lrcValue;
    }
}
