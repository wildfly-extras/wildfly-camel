package org.wildfly.camel.test.facebook.subA;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wildfly.camel.test.common.utils.TestUtils;

@WebServlet(name = "FakeFacebookAPIServlet", urlPatterns = {"/fake-facebook-api/*"})
public class FakeFacebookAPIServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (request.getPathInfo() == null) {
            return;
        }

        String responseJSON;

        switch (request.getPathInfo()) {
            case "/oauth-token":
                response.setHeader("facebook-api-version", "2.10");
                responseJSON = TestUtils.getResourceValue(FakeFacebookAPIServlet.class, "/facebook-token.json");
                break;
            case "/restfake-app/accounts/test-users":
                responseJSON = TestUtils.getResourceValue(FakeFacebookAPIServlet.class, "/facebook-test-users.json");
                break;
            default:
                throw new IllegalStateException("Unknown path: " + request.getPathInfo());
        }

        response.getOutputStream().print(responseJSON);
    }
}
