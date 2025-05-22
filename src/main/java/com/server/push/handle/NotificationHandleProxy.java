package com.server.push.handle;

import com.server.push.entity.Notification;
import com.server.push.enums.NotificationCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationHandleProxy {
    @Autowired private NotificationHandler notificationHandler;

    public void produce(Notification notification){
        notificationHandler.produceOneMessage(notification);
    }

    public void produce(List<Notification> notificationList){
        notificationHandler.produceOneMessage(notificationList);
    }

    public List<Notification> findHistoryNotifications(Integer userId, NotificationCode type,int offset){
        return notificationHandler.findHistoryNotification(userId,type,offset);
    }

    public List<Notification> findUnreadNotifications(Integer userId){
        return notificationHandler.findUnreadNotification(userId);
    }

    public void deleteMessage(Integer userId, NotificationCode type){
        notificationHandler.deleteNotification(userId,type);
    }

    public void deleteMessage(int userId, int targetId){
        notificationHandler.deleteNotification(userId,targetId);
    }
}

