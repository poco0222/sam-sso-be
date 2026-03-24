/**
 * @file 接收组分页查询 VO，收口为与自身语义一致的 PageVo 泛型
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.domain.vo;

/**
 * 接收组分页查询 VO。
 */
public class SysReceiveGroupVo extends PageVo<SysReceiveGroupVo> {
    /**
     * 接收编码
     */
    private String reCode;

    /**
     * 接收模式
     */
    private String reMode;

    /**
     * 状态
     */
    private String status;

    public String getReCode() {
        return reCode;
    }

    public void setReCode(String reCode) {
        this.reCode = reCode;
    }

    public String getReMode() {
        return reMode;
    }

    public void setReMode(String reMode) {
        this.reMode = reMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
