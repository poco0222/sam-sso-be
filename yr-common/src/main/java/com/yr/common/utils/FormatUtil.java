package com.yr.common.utils;

import java.text.DecimalFormat;

/**
 * <p>
 * description
 * </p>
 *
 * @author Youngron 2021-12-30 17:39
 * @version V1.0
 */
public class FormatUtil {

    private FormatUtil() {
    }

    /**
     * 转化bytes单位的数据为常见格式
     *
     * @param size
     * @return
     */
    public static String formatFileSize(Long size) {
        if (size != null && size > 0L) {
            String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10((double) size) / Math.log10(1024.0D));
            return (new DecimalFormat("#,##0.#")).format((double) size / Math.pow(1024.0D, (double) digitGroups)) + " " + units[digitGroups];
        } else {
            return "0";
        }
    }

}
