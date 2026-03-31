/**
 * @file 个人资料更新请求 DTO
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.web.controller.system.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

/**
 * 个人资料更新请求，只暴露自助资料字段。
 */
public class UpdateProfileRequest {

    /** 用户昵称。 */
    @Size(max = 30, message = "用户昵称长度不能超过30个字符")
    private String nickName;

    /** 手机号。 */
    @Size(max = 11, message = "手机号码长度不能超过11个字符")
    private String phonenumber;

    /** 邮箱。 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    /** 性别。 */
    private String sex;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
