package org.wildfly.camel.test.wordpress.subA;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.TestUtils;

@WebServlet(name = "WordpressApiMockServlet", urlPatterns = { "/wp/*" })
public class WordpressApiMockServlet extends HttpServlet {
    /**  */
    private static final long serialVersionUID = 1L;

    Logger LOG = LoggerFactory.getLogger(WordpressApiMockServlet.class);

    private static class ResponseMock {
        public static ResponseMock of(String... args) {
            int i = 0;
            Pattern p = Pattern.compile(args[i++]);
            Map<String, String> methodMap = new HashMap<>();
            for (; i < args.length;) {
                methodMap.put(args[i++], args[i++]);
            }
            return new ResponseMock(p, methodMap);
        }

        private final Map<String, String> methodMap;

        private final Pattern pattern;

        private ResponseMock(Pattern pattern, Map<String, String> methodMap) {
            super();
            this.pattern = pattern;
            this.methodMap = methodMap;
        }
    }

    private static final List<ResponseMock> responses = Arrays.asList( //
            ResponseMock.of("/v2/posts", //
                    "GET", "/wordpress/posts/list.json", //
                    "POST", "/wordpress/posts/create.json"),
            ResponseMock.of("/v2/posts/.*", //
                    "GET", "/wordpress/posts/single.json", //
                    "POST", "/wordpress/posts/update.json", //
                    "DELETE", "/wordpress/posts/delete.json"),
            ResponseMock.of("/v2/users", //
                    "GET", "/wordpress/users/list.json", //
                    "POST", "/wordpress/users/create.json"),
            ResponseMock.of("/v2/users/.*", //
                    "GET", "/wordpress/users/single.json", //
                    "POST", "/wordpress/users/update.json", //
                    "DELETE", "/wordpress/users/delete.json")

    );

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            return;
        }

        final String path = responses.stream() //
                .filter(rm -> {
                    boolean r = rm.pattern.matcher(pathInfo).matches();
                    LOG.warn("pattern " + rm.pattern.pattern() + " matches " + r);
                    return r;
                }) //
                .findFirst() //
                .map(rm -> rm.methodMap.get(request.getMethod())) //
                .get();
        final String json = TestUtils.getResourceValue(WordpressApiMockServlet.class, path);
        response.setContentType("application/json;charset=utf-8");
        response.getOutputStream().print(json);
    }
}
