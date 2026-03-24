/**
 * @file UUID 生成工具类
 * @author Codex
 * @date 2026-03-11
 */
package com.yr.system.utils;

import org.apache.commons.lang3.RandomUtils;

import java.util.UUID;

public class UUIDUtils {

    private static final char[] CHAR_MAP;

    static {
        CHAR_MAP = new char[62];
        for (int i = 0; i < 10; i++) {
            CHAR_MAP[i] = (char) ('0' + i);
        }
        for (int i = 10; i < 36; i++) {
            CHAR_MAP[i] = (char) ('a' + i - 10);
        }
        for (int i = 36; i < 62; i++) {
            CHAR_MAP[i] = (char) ('A' + i - 36);
        }
    }

    /**
     * 获取UUID参数入口类，传递需要获取的UUID位数，目前支持获取8、16、22、32位的UUID数据
     *
     * @param digit 位数
     * @return UUID生成结果
     */
    public static String getUUID(int digit) {
        return switch (digit) {
            case 8 -> getUUID8();
            case 16 -> getUUID16();
            case 22 -> getUUID22();
            case 32 -> getUUID32();
            default -> getUUID32();
        };
    }

    /**
     * 生成8位UUID
     *
     * @return 生成结果
     */
    private static String getUUID8() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            // 每组4位
            String str = uuid.substring(i * 4, i * 4 + 4);
            // 输出str在16进制下的表示
            int x = Integer.parseInt(str, 16);
            // 用该16进制数取模62（十六进制表示为314（14即E）），结果作为索引取出字符
            sb.append(CHAR_MAP[x % 62]);
        }
        return sb.toString();
    }

    /**
     * 生成16位UUID
     *
     * @return 生成结果
     */
    private static String getUUID16() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            // 每组2位
            String str = uuid.substring(i * 2, i * 2 + 2);
            // 输出str在16进制下的表示
            int x = Integer.parseInt(str, 16);
            // 用该16进制数取模62（十六进制表示为314（14即E）），结果作为索引取出字符
            sb.append(CHAR_MAP[x % 62]);
        }
        return sb.toString();
    }

    /**
     * 生成22位UUID
     * <p>
     * UUID是把128个二进制数，转换成32个16进制数的，每4个二进制数转换成一个16进制数。
     * 在UUID前补加一个16进制数，这样它就相当于是33位的16进制数（132位的2进制数），也就可以将其转换为22位的64进制数，起到缩减UUID长度的目的
     *
     * @return 生成结果
     */
    private static String getUUID22() {
        String uuid = "0" + UUID.randomUUID().toString().replace("-", "");
        return to64UUID(uuid);
    }

    /**
     * 生成32位UUID
     *
     * @return 生成结果
     */
    private static String getUUID32() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 转换64进制的UUID字符串
     *
     * @return 64进制UUID
     */
    private static String to64UUID(String uuid) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        int[] buff = new int[3];
        int uuidLength = uuid.length();
        for (int i = 0; i < uuidLength; i++) {
            index = i % 3;
            buff[index] = Integer.parseInt("" + uuid.charAt(i), 16);
            if (index == 2) {
                int index1 = (buff[0] << 2 | buff[1] >>> 2);
                int index2 = (buff[1] & 3) << 4 | buff[2];
                // 如果index1或index2值大于61，则在0-61中随机取出一个值作为index的值，这里的加密使用base62加密，不使用base64
                if (index1 > 61) {
                    index1 = RandomUtils.nextInt(0, 61);
                }
                if (index2 > 61) {
                    index2 = RandomUtils.nextInt(0, 61);
                }
                sb.append(CHAR_MAP[index1]);
                sb.append(CHAR_MAP[index2]);
            }
        }
        return sb.toString();
    }

}
