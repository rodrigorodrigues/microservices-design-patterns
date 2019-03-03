package com.springboot.edgeserver.config;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.netflix.zuul.context.RequestContext.getCurrentContext;
import static org.springframework.util.ReflectionUtils.rethrowRuntimeException;

@Slf4j
@Profile("heroku")
@Component
public class ChangeRequestBodyFilter extends ZuulFilter {

    @Value("${server.port}")
    private int port;

    private final String[] paths = {"/eureka", "/monitoring"};

    private final Pattern hrefPattern = Pattern.compile("href=\"(.*?)\"|href=(.*?)[>,\\s]");

    private final Pattern srcPattern = Pattern.compile("src=\"(.*?)\"|src=(.*?)[>,\\s]");

    private final Pattern httpPattern = Pattern.compile("http://(.*?)[0-9]{4}");

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 999;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = getCurrentContext();
        String servletPath = context.getRequest().getServletPath();
        return Stream.of(paths).anyMatch(p -> servletPath.startsWith(p) && !servletPath.endsWith(".js") && !servletPath.endsWith(".css"));
    }

    @Override
    public Object run() {
        try {
            RequestContext context = getCurrentContext();
            InputStream in = context.getResponseDataStream();
            if (in != null) {
                String body = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
                body = replaceHtml(context, body);
                context.setResponseDataStream(new ByteArrayInputStream(body.getBytes("UTF-8")));
            }
        } catch (SocketTimeoutException ste) {
            log.warn("Error socket timeout on method run", ste);
        } catch (IOException e) {
            log.error("Error on method run", e);
            rethrowRuntimeException(e);
        }
        return null;
    }

    public String replaceHtml(RequestContext context, String body) {
        String newPath = String.format("http://localhost:%d%s", port, getRoutePath(context));

        Matcher hrefMatcher = hrefPattern.matcher(body);
        Matcher srcMatcher = srcPattern.matcher(body);
        body = replaceByMatcher(body, newPath, hrefMatcher, 5);
        body = replaceByMatcher(body, newPath, srcMatcher, 4);

        return body;
    }

    private String replaceByMatcher(String body, String newPath, Matcher matcher, int index) {
        while (matcher.find()) {
            String group = matcher.group();
            log.debug("Group:before: {}", group);
            Matcher httpMatcher = httpPattern.matcher(group);
            String newGroup;
            if (httpMatcher.find()) {
                newGroup = httpMatcher.replaceAll(newPath);
            } else {
                if ((index + 1) >= group.length()) {
                    newGroup = group.substring(0, index) + newPath + group.substring(index);
                } else {
                    if (group.charAt(index) == '"') {
                        ++index;
                    }
                    newGroup = group.substring(0, index) + newPath + (group.charAt(index) != '/' ? "/" : "") + group.substring(index);
                }
            }
            log.debug("Group:after: {}", newGroup);
            body = body.replaceFirst(group, newGroup);
        }
        return body;
    }

/*
    public String replaceHtml(RequestContext context, String body) {
        String newPath = String.format("http://localhost:%d%s", port, getRoutePath(context));

        return body.replaceAll("href=\"http://localhost:[0-9]{4}", "href=\""+newPath)
                .replaceAll("href=http://localhost:[0-9]{4}", "href="+newPath)
                .replaceAll("href=\"/", "href=\""+newPath+"/")
                .replaceAll("href=/", "href="+newPath+"/")
                .replaceAll("href=", "href="+newPath+"/")
                .replaceAll("src=\"/", "src=\"" + newPath + "/")
                .replaceAll("src=\"", "src=\"" + newPath + "/")
                .replaceAll("src=", "src=" + newPath + "/");

    }
*/

    private String getRoutePath(RequestContext context) {
        String servletPath = context.getRequest().getServletPath();
        return Stream.of(paths)
                .filter(servletPath::startsWith)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, String.format("Filter is applied for invalid path(%s)", servletPath)));
    }
}
