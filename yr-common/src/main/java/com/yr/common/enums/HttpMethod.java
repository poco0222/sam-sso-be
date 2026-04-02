package com.yr.common.enums;

import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求方式
 *
 * @author PopoY
 */
public enum HttpMethod {
    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;

    private static final Map<String, HttpMethod> MAPPING_S = new HashMap<>(16);

    static {
        for (HttpMethod httpMethod : values()) {
            MAPPING_S.put(httpMethod.name(), httpMethod);
        }
    }

    @Nullable
    public static HttpMethod resolve(@Nullable String method) {
        return (method != null ? MAPPING_S.get(method) : null);
    }

    public boolean matches(String method) {
        return (this == resolve(method));
    }
}
