package com.yr.common.utils.sign;

import com.yr.common.exception.CustomException;
import com.yr.common.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <p>
 * sha1 工具类
 * </p>
 *
 * @author PopoY 2021-12-10 17:18
 * @version V1.0
 */
public class Sha1Utils {

    private Sha1Utils() {
    }

    public static String encryption(String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] byteArray = data.getBytes("UTF-8");
            byte[] md5Bytes = sha.digest(byteArray);
            StringBuilder hexValue = new StringBuilder();
            for (byte md5Byte : md5Bytes) {
                int val = md5Byte & 0xff;
                if (val < 16) {
                    hexValue.append("0");
                }
                hexValue.append(Integer.toHexString(val));
            }
            return hexValue.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new CustomException("sha1 encryption fail：", e);
        }
    }

}
