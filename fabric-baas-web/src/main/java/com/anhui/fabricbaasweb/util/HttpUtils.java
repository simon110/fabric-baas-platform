package com.anhui.fabricbaasweb.util;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

public class HttpUtils {
    public static HttpServletRequest getHttpServletRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return ((ServletRequestAttributes) Objects.requireNonNull(attributes)).getRequest();
    }
}

