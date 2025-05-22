package com.server.schedule;

import com.server.dao.message.MessageDao;
import com.server.dao.notification.NotificationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class cleanNotificationAndMessageSchedule {
    @Autowired private NotificationDao notificationDao;
    @Autowired private MessageDao messageDao ;


}
