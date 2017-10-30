package com.company.websocket.lanucher;


import com.company.websocket.server.WebSocketServer;

public class Lanucher {

    public static void main(String[] args) throws Exception {
        // 启动WebSocket
        new WebSocketServer().start(WebSocketServer.WEBSOCKET_PORT);
    }

}
