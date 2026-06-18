package com.interview_platform_backend.interview_platform_backend.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.regex.Pattern;

/**
 * Wrapper that sanitizes request parameter values by stripping potentially
 * dangerous HTML/script patterns.
 */
public class XssSanitizingRequestWrapper extends HttpServletRequestWrapper {

    private static final Pattern SCRIPT_TAG = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EVENT_HANDLER = Pattern.compile("on\\w+\\s*=\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PROTOCOL = Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);

    public XssSanitizingRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return sanitize(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] sanitized = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sanitized[i] = sanitize(values[i]);
        }
        return sanitized;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return sanitize(value);
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = SCRIPT_TAG.matcher(value).replaceAll("");
        sanitized = EVENT_HANDLER.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PROTOCOL.matcher(sanitized).replaceAll("");
        return sanitized;
    }
}
