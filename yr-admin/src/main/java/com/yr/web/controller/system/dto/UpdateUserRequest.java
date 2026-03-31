/**
 * @file 用户普通编辑请求 DTO
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.web.controller.system.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 用户普通编辑请求，只暴露基础资料修改所需字段。
 */
public class UpdateUserRequest {

    /** 目标用户 ID。 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 部门 ID。 */
    private Long deptId;

    /** 登录账号。 */
    @NotBlank(message = "用户账号不能为空")
    @Size(max = 30, message = "用户账号长度不能超过30个字符")
    private String userName;

    /** 用户昵称。 */
    @Size(max = 30, message = "用户昵称长度不能超过30个字符")
    private String nickName;

    /** 邮箱。 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    /** 手机号。 */
    @Size(max = 11, message = "手机号码长度不能超过11个字符")
    private String phonenumber;

    /** 性别。 */
    private String sex;

    /** 头像。 */
    private String avatar;

    /** 普通备注。 */
    private String remark;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
