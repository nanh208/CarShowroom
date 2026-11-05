package com.ares.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import java.util.regex.Pattern;

/** Cache rules for public catalog data, private API responses, and immutable uploaded files. */
@Configuration
public class CacheHeadersConfig {
    private static final Pattern SHORT_CACHE = Pattern.compile("^/api/(categories|promotions)$");
    private static final Pattern PUBLIC_API = Pattern.compile("^/api/(cars(?:/\\d+)?|categories|reviews|promotions)$");

    @Bean
    public FilterRegistrationBean<Filter> cacheHeaders() {
        Filter filter = (request, response, chain) -> {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String path = path(httpRequest);
            boolean readOnly = "GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod());
            boolean authorized = httpRequest.getHeader("Authorization") != null;

            if (path.startsWith("/uploads/")) {
                httpResponse.setHeader("Cache-Control", "public, max-age=31536000, immutable");
            } else if (path.startsWith("/api/")) {
                httpResponse.setHeader("Vary", "Authorization, Accept-Encoding");
                if (!readOnly || authorized || !PUBLIC_API.matcher(path).matches()) {
                    httpResponse.setHeader("Cache-Control", "private, no-store");
                } else if (SHORT_CACHE.matcher(path).matches()) {
                    httpResponse.setHeader("Cache-Control", "public, max-age=300, stale-while-revalidate=600");
                } else {
                    httpResponse.setHeader("Cache-Control", "no-cache");
                }
            }
            chain.doFilter(request, response);
        };

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> publicApiEtags() {
        ShallowEtagHeaderFilter filter = new ShallowEtagHeaderFilter() {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return request.getHeader("Authorization") != null
                        || !PUBLIC_API.matcher(path(request)).matches();
            }
        };
        filter.setWriteWeakETag(true);
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    private static String path(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath == null || contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
    }
}
