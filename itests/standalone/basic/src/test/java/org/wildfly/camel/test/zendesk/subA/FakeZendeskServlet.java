package org.wildfly.camel.test.zendesk.subA;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wildfly.camel.test.common.utils.TestUtils;

@WebServlet(name = "FakeZendeskServlet", urlPatterns = {"/fake-api/api/v2/*"})
public class FakeZendeskServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (request.getPathInfo().equals("/tickets.json")) {
            String resource = TestUtils.getResourceValue(FakeZendeskServlet.class, "/tickets.json");
            response.getOutputStream().print(resource);
        } else {
            throw new IllegalStateException("Unknown path: " + request.getPathInfo());
        }
    }
}
