package com.server.push.service;

import com.server.push.dto.request.NotificationForVideoResponse;
import com.server.push.dto.response.HistoryNotificationResponse;
import com.server.push.enums.NotificationCode;
import com.server.push.service.impl.NotificationServiceImpl;

import java.util.List;

public interface NotificationService {

    /**
        推送 实现是我对他人的操作 搜集数据  推送给他人
     */
    void likeToVideoNotices(Integer myId,Integer authorId,Integer videoId);
    void likeToCommentNotices(Integer myId,Integer authorId,String commentId); //要定位
    void commentToCommentNotices(Integer myId,Integer authorId,String commentId);
    void followToAuthorNotices(Integer myId,Integer authorId);
    void letterToOtherNotices(Integer myId,Integer otherId);
    void newDevelopmentToFunNotices(Integer myId,Integer videoId);
    void auditingStatusNotification(Integer userId,Integer videoId,boolean isPass,String title);


    /**
        历史通知
     */
    List<NotificationForVideoResponse> getLikeVideoNotification(Integer userId, int offset) throws InterruptedException;
    List<NotificationServiceImpl.NotificationReplyResponse> getLikeCommentNotification(Integer userId, int offset) throws InterruptedException;
    List<NotificationServiceImpl.NotificationReplyResponse>  getReplyCommentNotification(Integer userId, int offset) throws InterruptedException;
    List<HistoryNotificationResponse<Integer>> getFollowNotification(Integer userId,int offset) throws InterruptedException;


    /**
     * 删除历史消息
     */
    void deleteNotifications(Integer userId, NotificationCode type);

    /**
     *这是专用于删除 notification type=4/5
     */
    void deleteNotifications(int userId,int targetId);
}
