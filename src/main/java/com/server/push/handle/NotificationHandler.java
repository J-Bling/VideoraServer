package com.server.push.handle;

import com.server.push.entity.Notification;
import com.server.push.enums.NotificationCode;
import org.springframework.web.socket.WebSocketHandler;
import java.util.List;

public interface NotificationHandler extends WebSocketHandler {
    void produceOneMessage(Notification notification);
    void produceOneMessage(List<Notification> notificationList);
    List<Notification> findUnreadNotification(Integer userId);
    List<Notification> findHistoryNotification(Integer userId, NotificationCode type, int offset);
}
