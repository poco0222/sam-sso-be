/**
 * @file 同步任务客户端观测摘要 DTO
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 提供 sync-task 控制台客户端维度观测卡片所需的最小摘要视图。
 */
public class SsoSyncTaskClientSummaryView {

    /** 客户端编码。 */
    private String clientCode;

    /** 最近任务 ID。 */
    private Long latestTaskId;

    /** 最近批次号。 */
    private String latestBatchNo;

    /** 最近失败任务 ID。 */
    private Long latestFailedTaskId;

    /** 最近失败批次号。 */
    private String latestFailedBatchNo;

    /** 最近成功时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date latestSuccessTime;

    /** 失败任务数，包含 FAILED 与 PARTIAL_SUCCESS。 */
    private Long failedTaskCount;

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public Long getLatestTaskId() {
        return latestTaskId;
    }

    public void setLatestTaskId(Long latestTaskId) {
        this.latestTaskId = latestTaskId;
    }

    public String getLatestBatchNo() {
        return latestBatchNo;
    }

    public void setLatestBatchNo(String latestBatchNo) {
        this.latestBatchNo = latestBatchNo;
    }

    public Long getLatestFailedTaskId() {
        return latestFailedTaskId;
    }

    public void setLatestFailedTaskId(Long latestFailedTaskId) {
        this.latestFailedTaskId = latestFailedTaskId;
    }

    public String getLatestFailedBatchNo() {
        return latestFailedBatchNo;
    }

    public void setLatestFailedBatchNo(String latestFailedBatchNo) {
        this.latestFailedBatchNo = latestFailedBatchNo;
    }

    public Date getLatestSuccessTime() {
        return latestSuccessTime;
    }

    public void setLatestSuccessTime(Date latestSuccessTime) {
        this.latestSuccessTime = latestSuccessTime;
    }

    public Long getFailedTaskCount() {
        return failedTaskCount;
    }

    public void setFailedTaskCount(Long failedTaskCount) {
        this.failedTaskCount = failedTaskCount;
    }
}
