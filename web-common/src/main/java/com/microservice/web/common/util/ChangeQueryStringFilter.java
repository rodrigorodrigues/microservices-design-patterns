package com.microservice.web.common.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ChangeQueryStringFilter<T> implements Filter {
    public abstract Class<T> getObjectType();

    public abstract Class<? extends Annotation> getEntityType();

    private HttpServletRequestWrapper changeQueryString(HttpServletRequest request) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public Map<String, String[]> getParameterMap() {
                Map<String, String[]> parameterMap = super.getParameterMap();
                if (parameterMap.size() == 1 && (parameterMap.values().iterator().next()[0] == null || parameterMap.values().iterator().next()[0].isBlank())) {
                    parameterMap = processMap(parameterMap, getObjectType(), null);
                }
                return parameterMap;
            }

        };
    }

    private Map<String, String[]> processMap(Map<String, String[]> parameterMap, Class<?> objectType, String recursive) {
        Map<String, String[]> parameterMapCopy = new HashMap<>();
        for (Field field : objectType.getDeclaredFields()) {
            if (field.getType().isAssignableFrom(String.class)) {
                parameterMapCopy.put((recursive != null ? recursive + field.getName() : field.getName()), parameterMap.keySet().toArray(new String[] {}));
            } else if (field.getType().isAnnotationPresent(getEntityType())) {
                parameterMapCopy.putAll(processMap(parameterMap, field.getType(), (recursive != null ? recursive + "." + field.getName() + "." : field.getName() + ".")));
            }
        }
        return parameterMapCopy;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(changeQueryString((HttpServletRequest) request), response);
    }
}
