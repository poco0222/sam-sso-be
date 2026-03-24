package com.yr.common.exception.user;

/**
 * 用户密码不正确或不符合规范异常类
 *
 * @author Youngron
 */
public class UserPasswordRetryLimitExceedException extends UserException {
    private static final long serialVersionUID = 1L;

    public UserPasswordRetryLimitExceedException(Integer times, Integer time) {
        super("user.password.retry.limit.exceed", new Object[]{times, time});
    }
}
