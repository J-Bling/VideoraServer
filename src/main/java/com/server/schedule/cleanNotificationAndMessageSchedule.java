package com.server.schedule;

import com.server.dao.message.MessageDao;
import com.server.dao.notification.NotificationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class cleanNotificationAndMessageSchedule {
    @Autowired private NotificationDao notificationDao;
    @Autowired private MessageDao messageDao ;

    private final long SAVE_EXPIRED = 3L *30*24*60*60*1000;


    @Scheduled(cron = "3 0 0 * * ?")
    public void cleanNotification(){
        long expired = System.currentTimeMillis() + SAVE_EXPIRED;
        notificationDao.cleanNotificationByCreated(expired);
    }

    @Scheduled(cron = "4 0 0 * * ?")
    public void cleanMessage(){
        long expired = System.currentTimeMillis() + SAVE_EXPIRED;
        messageDao.cleanMessage(expired);
    }
}
