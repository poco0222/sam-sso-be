package com.yr.common.exception.user;

/**
 * 用户密码不正确或不符合规范异常类
 *
 * @author PopoY
 */
public class UserPasswordRetryLimitCountException extends UserException {
    private static final long serialVersionUID = 1L;

    public UserPasswordRetryLimitCountException(Integer times) {
        super("user.password.retry.limit.count", new Object[]{times});
    }
}
