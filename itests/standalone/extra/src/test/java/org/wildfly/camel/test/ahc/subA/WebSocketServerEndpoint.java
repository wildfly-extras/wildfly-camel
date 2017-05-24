package org.wildfly.camel.test.ahc.subA;

import java.util.logging.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo")
public class WebSocketServerEndpoint {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @OnOpen
    public void onConnectionOpen(Session session) {
        logger.info("onConnectionOpen: " + session.getId());
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        logger.info("onMessage: " + message + " in " + session.getId());
        return "Hello " + message;
    }

    @OnClose
    public void onConnectionClose(Session session) {
        logger.info("onConnectionClose: " + session.getId());
    }
}
