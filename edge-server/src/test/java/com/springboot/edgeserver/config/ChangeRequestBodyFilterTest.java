package com.springboot.edgeserver.config;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeRequestBodyFilterTest {

    String html = "<html><head><base href=http://localhost:9000/><meta charset=utf-8><meta http-equiv=X-UA-Compatible content=\"IE=edge,chrome=1\"><meta name=format-detection content=\"telephone=no,email=no\"><meta name=theme-color content=#42d3a5><link rel=\"shortcut icon\" href=assets/img/favicon.png type=image/png><title>Spring Boot Admin</title>" +
            "<link href=assets/css/chunk-common.9808b72e.css rel=preload as=style>\n" +
            "<link href=assets/css/sba-core.360886fb.css rel=preload as=style>\n" +
            "<link href=assets/js/chunk-common.82a7af35.js rel=preload as=script>\n" +
            "<link href=assets/js/chunk-vendors.1df687d0.js rel=preload as=script>\n" +
            "<link href=assets/js/sba-core.ab2ed73c.js rel=preload as=script>\n" +
            "<link href=assets/css/chunk-common.9808b72e.css rel=stylesheet>\n" +
            "<link href=assets/css/sba-core.360886fb.css rel=stylesheet>\n" +
            "<link rel=\"stylesheet\" href=\"eureka/css/wro.css\">\n" +
            "<base href=\"/\">" +
            "</head><body><div id=app><a href=\"\">Home</a></div><script>var SBA = {\n" +
            "        uiSettings: {\"title\":\"Spring Boot Admin\",\"brand\":\"<img src=\\\"assets\\/img\\/icon-spring-boot-admin.svg\\\"><span>Spring Boot Admin<\\/span>\"},\n" +
            "        user: {\"name\":\"admin@gmail.com\"},\n" +
            "        extensions: [],\n" +
            "        \n" +
            "        use: function (ext) {\n" +
            "            this.extensions.push(ext);\n" +
            "        }\n" +

            "    }</script><script src=\"http://172.168.0.1:9000/assets/js/chunk-vendors.1df687d0.js\"></script><script src=assets/js/chunk-common.82a7af35.js></script><script src=assets/js/sba-core.ab2ed73c.js></script></body></html>";

    @Test
    public void testRegexHtml() {
        ChangeRequestBodyFilter changeRequestBodyFilter = new ChangeRequestBodyFilter();

        ReflectionTestUtils.setField(changeRequestBodyFilter, "port", 9006);

        RequestContext context = new RequestContext();
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setServletPath("/monitoring");
        context.setRequest(mockHttpServletRequest);

        String newHtml = changeRequestBodyFilter.replaceHtml(context, html);

        assertThat(newHtml).contains("<base href=http://localhost:9006/monitoring/>");
        assertThat(newHtml).contains("<script src=\"http://localhost:9006/monitoring/assets/js/chunk-vendors.1df687d0.js\">");
        assertThat(newHtml).contains("<link rel=\"stylesheet\" href=\"http://localhost:9006/monitoring/eureka/css/wro.css\">");
        assertThat(newHtml).contains("<base href=\"http://localhost:9006/monitoring/\">");
        assertThat(newHtml).contains("<a href=\"http://localhost:9006/monitoring\">Home</a>");
    }
}