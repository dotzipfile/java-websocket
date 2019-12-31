package com.dotzipfile.websocket;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Main {

    public static void main(String[] args) throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        httpServer.createContext("/chat", new SocketHandler());
        httpServer.createContext("/ui", new UiHandler());
        httpServer.setExecutor(null); // creates a default executor
        httpServer.start();
    }

    static class SocketHandler implements HttpHandler {

        private static final String MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

        @Override
        public void handle(HttpExchange t) throws IOException {
            Headers requestHeaders = t.getRequestHeaders();

            String webSocketAccept = generateWebSocketAccept(requestHeaders.getFirst("Sec-WebSocket-Key") + MAGIC_STRING);

            Headers responseHeaders = t.getResponseHeaders();
            responseHeaders.set("Upgrade", "websocket");
            responseHeaders.set("Connection", "Upgrade");
            responseHeaders.set("Sec-WebSocket-Accept", webSocketAccept);

            t.sendResponseHeaders(101, -1);
            OutputStream os = t.getResponseBody();
            os.close();
        }

        private static String generateWebSocketAccept(String webSocketKeyMagicString) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
                messageDigest.reset();
                messageDigest.update(webSocketKeyMagicString.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(messageDigest.digest());
            } catch(NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    static class UiHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Websockets UI</title>" +
                    "<script>function fire() {var websocket = new WebSocket('ws://localhost:8080/chat');}</script></head>" +
                    "<body><button onclick=\"fire()\">Fire</button></body></html>";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
