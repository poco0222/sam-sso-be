package com.yr.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NumberUtils {

    private static final Logger logger = LoggerFactory.getLogger(NumberUtils.class);


    public static Long safeParseLong(String val) {
        Long ret = null;
        if (StringUtils.isNotBlank(val)) {
            try {
                ret = Long.parseLong(val);
            } catch (NumberFormatException nfe) {
                logger.error("Convert STRING to LONG failed. Raw Value : {}", val);
            }
        }
        return ret;
    }

    /**
     * 整数转十六进制
     */
    public static String intToHex(int value) {
        return Integer.toHexString(value).toUpperCase();
    }

    /**
     * 长整数转十六进制
     */
    public static String longToHex(long value) {
        return Long.toHexString(value).toUpperCase();
    }

    /**
     * 浮点数转十六进制（IEEE 754位表示）
     */
    public static String floatToHex(float value) {
        int bits = Float.floatToIntBits(value);
        return Integer.toHexString(bits).toUpperCase();
    }

    /**
     * 双精度浮点数转十六进制（IEEE 754位表示）
     */
    public static String doubleToHex(double value) {
        long bits = Double.doubleToLongBits(value);
        return Long.toHexString(bits).toUpperCase();
    }

    /**
     * 通用数值转十六进制
     */
    public static String numberToHex(Number value) {
        if (value instanceof Float) {
            return floatToHex(value.floatValue());
        } else if (value instanceof Double) {
            return doubleToHex(value.doubleValue());
        } else if (value instanceof Long) {
            return longToHex(value.longValue());
        } else {
            return intToHex(value.intValue());
        }
    }

    /**
     * 十六进制字符串转回整数
     */
    public static int hexToInt(String hex) {
        // 移除0x前缀
        String cleanHex = hex.replace("0x", "").replace("0X", "");
        return (int) Long.parseLong(cleanHex, 16);
    }

    /**
     * 十六进制字符串转回长整数
     */
    public static long hexToLong(String hex) {
        // 移除0x前缀
        String cleanHex = hex.replace("0x", "").replace("0X", "");
        return Long.parseLong(cleanHex, 16);
    }

    /**
     * 十六进制字符串转回浮点数
     */
    public static float hexToFloat(String hex) {
        // 移除0x前缀
        String cleanHex = hex.replace("0x", "").replace("0X", "");
        int bits = (int) Long.parseLong(cleanHex, 16);
        return Float.intBitsToFloat(bits);
    }

    /**
     * 十六进制字符串转回双精度浮点数
     */
    public static double hexToDouble(String hex) {
        // 移除0x前缀
        String cleanHex = hex.replace("0x", "").replace("0X", "");
        long bits = Long.parseLong(cleanHex, 16);
        return Double.longBitsToDouble(bits);
    }

    /**
     * 安全的十六进制字符串转长整数
     */
    public static Long safeHexToLong(String hex) {
        Long ret = null;
        if (StringUtils.isNotBlank(hex)) {
            try {
                ret = hexToLong(hex);
            } catch (NumberFormatException nfe) {
                logger.error("Convert HEX to LONG failed. Raw Value : {}", hex);
            }
        }
        return ret;
    }

}
