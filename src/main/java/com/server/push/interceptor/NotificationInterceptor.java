package com.server.push.interceptor;

import com.server.entity.constant.WebConstant;
import com.server.util.JwtUtil;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

@Component
public class NotificationInterceptor extends HttpSessionHandshakeInterceptor {
    /**
     * 用于ws握手请求的拦截器 可以用于检查握手请求和响应  以及将目标熟悉传递 WebSocketHandler
     */

    public NotificationInterceptor(){}


    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes)
            throws Exception
    {
        String query= request.getURI().getQuery();
        String token=query.split(WebConstant.WEB_SOCKET_QUERY,2)[1];
        String id= JwtUtil.validateAndGetToken(token);
        if(id==null) return false;
        attributes.put(WebConstant.WEBSOCKET_USER_ID,id);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        super.afterHandshake(request,response,wsHandler,exception);
    }
}
