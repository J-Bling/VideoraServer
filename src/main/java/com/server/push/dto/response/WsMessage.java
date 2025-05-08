package com.server.push.dto.response;

import org.springframework.web.socket.TextMessage;

public class WsMessage{

    public static TextMessage transTextMessage(String data){
        return new TextMessage(data.getBytes());
    }
}
