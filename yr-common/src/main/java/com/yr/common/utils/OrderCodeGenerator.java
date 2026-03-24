package com.yr.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 通用单据编码生成工具类
 * 支持前缀+日期+流水号的编码规则
 *
 * @author tiger
 * @version 1.0
 * @since 2025-10-15 11:12
 */
@Slf4j
@Component
public class OrderCodeGenerator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 缓存锁，保证相同编码规则的生成过程线程安全
    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * 生成单据编码
     *
     * @param prefix       编码前缀
     * @param datePattern  日期格式(yy/yyyy/yyyymm/yyyymmdd)
     * @param serialLength 流水号位数
     * @param tableName    表名
     * @param orderField   订单号字段名
     * @return 生成的单据编码
     */
    public String generateOrderCode(String prefix, String datePattern, int serialLength,
                                    String tableName, String orderField) {
        // 构建锁的key
        String lockKey = prefix + "_" + datePattern + "_" + tableName + "_" + orderField;
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            // 获取当前日期
            String currentDate = formatDate(datePattern);
            // 构造查询条件：前缀+当前日期
            String codePrefix = prefix + currentDate;

            // 查询当日最大流水号
            String maxSerialNo = getMaxSerialNoFromTable(tableName, orderField, codePrefix);

            // 生成新的流水号
            String newSerialNo = generateNextSerialNo(maxSerialNo, codePrefix, serialLength);

            return codePrefix + newSerialNo;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从数据库表中查询最大流水号
     *
     * @param tableName  表名
     * @param orderField 订单号字段名
     * @param codePrefix 编码前缀
     * @return 最大流水号
     */
    private String getMaxSerialNoFromTable(String tableName, String orderField, String codePrefix) {
        try {
            String sql = "SELECT MAX(" + orderField + ") FROM " + tableName +
                    " WHERE " + orderField + " LIKE ?";
            return jdbcTemplate.queryForObject(sql, String.class, codePrefix + "%");
        } catch (Exception e) {
            log.warn("查询最大流水号失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据指定格式格式化当前日期
     *
     * @param pattern 日期模式 (yy/yyyy/yyyymm/yyyymmdd)
     * @return 格式化后的日期字符串
     */
    private String formatDate(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        switch (pattern.toLowerCase()) {
            case "yy":
                sdf.applyPattern("yy");
                break;
            case "yyyy":
                sdf.applyPattern("yyyy");
                break;
            case "yyyymm":
                sdf.applyPattern("yyyyMM");
                break;
            case "yyyymmdd":
                sdf.applyPattern("yyyyMMdd");
                break;
            default:
                sdf.applyPattern("yyyyMMdd");
                break;
        }
        return sdf.format(new Date());
    }

    /**
     * 生成下一个流水号
     *
     * @param maxSerialNo 数据库中的最大流水号
     * @param prefix      前缀(包括日期部分)
     * @param length      流水号长度
     * @return 新的流水号
     */
    private String generateNextSerialNo(String maxSerialNo, String prefix, int length) {
        int nextNumber = 1; // 默认从1开始

        if (maxSerialNo != null && !maxSerialNo.isEmpty()) {
            // 从现有最大编号中提取流水号部分
            String currentSerial = maxSerialNo.substring(prefix.length());
            try {
                nextNumber = Integer.parseInt(currentSerial) + 1;
            } catch (NumberFormatException e) {
                // 如果解析失败，默认从1开始
                nextNumber = 1;
            }
        }

        // 格式化为指定位数的流水号
        return String.format("%0" + length + "d", nextNumber);
    }
}
