package com.server.push.service.impl;

import com.server.dao.notification.NotificationDao;
import com.server.dao.record.RecordDao;
import com.server.dto.response.comment.CommentResponse;
import com.server.dto.response.user.UserResponse;
import com.server.dto.response.video.VideoDataResponse;
import com.server.push.dto.request.NotificationForComment;
import com.server.push.dto.request.NotificationForVideoResponse;
import com.server.push.dto.response.HistoryNotificationResponse;
import com.server.push.entity.Notification;
import com.server.push.enums.NotificationCode;
import com.server.push.handle.NotificationHandleProxy;
import com.server.push.service.NotificationService;
import com.server.service.commentservice.CommentService;
import com.server.service.userservice.UserDataService;
import com.server.service.videoservice.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired private NotificationHandleProxy proxy;
    @Autowired private RecordDao recordDao;
    @Autowired private VideoService videoService;
    @Autowired private CommentService commentService;
    @Autowired private UserDataService userDataService;
    @Autowired private NotificationDao notificationDao;


    public static final ConcurrentLinkedQueue<String> UNREAD_NOTIFICATIONS=new ConcurrentLinkedQueue<>();

    private Map<Integer,List<Notification>> classifyForTagId(List<Notification> notifications){
        if(notifications==null || notifications.isEmpty()) return null;
        Map<Integer,List<Notification>> classify = new HashMap<>();

        for(Notification notification : notifications){
            classify.computeIfAbsent(notification.getTag_id(),k->new ArrayList<>()).add(notification);
        }

        return classify;
    }

    @Override
    public List<NotificationForVideoResponse> getLikeVideoNotification(Integer userId, int offset)
            throws InterruptedException {
        List<Notification> notificationList= proxy.findHistoryNotifications(userId,NotificationCode.LIKED_FOR_VIDEO,offset);
        Map<Integer,List<Notification>> notificationMapList= classifyForTagId(notificationList);

        if(notificationMapList==null) return null;

        List<NotificationForVideoResponse> notificationForVideoResponses = new ArrayList<>();
        for(Map.Entry<Integer,List<Notification>> notificationEntry : notificationMapList.entrySet()){
            Integer videoId = notificationEntry.getKey();
            List<Notification> notifications = notificationEntry.getValue();

            VideoDataResponse videoDataResponse= videoService.getVideoResponseData(videoId,null);
            if(videoDataResponse==null) continue;

            List<HistoryNotificationResponse<Integer>> notificationResponses =new ArrayList<>();
            for(Notification notification : notifications){
                UserResponse userResponse = userDataService.getUserResponseData(notification.getTarget_id());
                notificationResponses.add(new HistoryNotificationResponse<>(notification,userResponse));

                if(!notification.getIs_read()){
                    UNREAD_NOTIFICATIONS.offer(notification.getMessage_id());
                }
            }

            NotificationForVideoResponse response = new NotificationForVideoResponse(videoDataResponse);
            response.setNotificationWithUser(notificationResponses);

            notificationForVideoResponses.add(response);
        }

        return notificationForVideoResponses;
    }

    private List<NotificationForComment> getCommentInteractionNotification(Integer userId,int offset,NotificationCode code)
            throws InterruptedException {
        List<Notification> notificationList = proxy.findHistoryNotifications(userId,code,offset);
        Map<Integer,List<Notification>> notificationCommentMap=classifyForTagId(notificationList);

        if(notificationList==null) return null;

        List<NotificationForComment> notificationForCommentList = new ArrayList<>();
        for(Map.Entry<Integer,List<Notification>> notificationEntry : notificationCommentMap.entrySet()){
            Integer commentId = notificationEntry.getKey();
            List<Notification> notifications = notificationEntry.getValue();

            CommentResponse comment= commentService.getPublicCommentByRedis(commentId);
            if(comment==null) continue;

            VideoDataResponse video = videoService.getVideoResponseData(comment.getVideo_id(),null);
            NotificationForComment notificationForComment= new NotificationForComment(comment,video);

            List<HistoryNotificationResponse<Integer>> historyNotificationResponses = new ArrayList<>();
            for(Notification notification : notifications){
                if(notification!=null){
                    UserResponse user = userDataService.getUserResponseData(notification.getTarget_id());
                    historyNotificationResponses.add(new HistoryNotificationResponse<>(notification,user));

                    if(!notification.getIs_read()){
                        UNREAD_NOTIFICATIONS.offer(notification.getMessage_id());
                    }
                }
            }

            notificationForComment.setNotificationWithUser(historyNotificationResponses);
            notificationForCommentList.add(notificationForComment);
        }

        return notificationForCommentList;
    }

    @Override
    public List<NotificationForComment> getLikeCommentNotification(Integer userId, int offset) throws InterruptedException {
        return getCommentInteractionNotification(userId,offset,NotificationCode.LIKED_FOR_COMMENT);
    }

    @Override
    public List<NotificationForComment> getReplyCommentNotification(Integer userId, int offset) throws InterruptedException {
        return getCommentInteractionNotification(userId,offset,NotificationCode.REPLY_FOR_COMMENT);
    }

    @Override
    public List<HistoryNotificationResponse<Integer>> getFollowNotification(Integer userId, int offset)
            throws InterruptedException {
        List<Notification> notifications= proxy.findHistoryNotifications(userId,NotificationCode.FOLLOWED_FOR,offset);
        if(notifications==null || notifications.isEmpty()) return null;

        List<HistoryNotificationResponse<Integer>> historyNotificationResponses = new ArrayList<>();
        for(Notification notification : notifications){
            UserResponse user =userDataService.getUserResponseData(notification.getTarget_id());
            historyNotificationResponses.add(new HistoryNotificationResponse<>(notification,user));

            if(!notification.getIs_read()){
                UNREAD_NOTIFICATIONS.offer(notification.getMessage_id());
            }
        }

        return historyNotificationResponses;
    }


    @Override
    public void likeToVideoNotices(Integer myId, Integer authorId, Integer videoId) {
        Notification notification = new Notification();
        notification.setUser_id(authorId);
        notification.setTarget_id(myId);
        notification.setTag_id(videoId);
        notification.setCreated(System.currentTimeMillis());
        notification.setIs_read(false);
        notification.setMessage_id(UUID.randomUUID().toString());
        notification.setMessage(NotificationCode.LIKED_FOR_VIDEO.getDescription());
        notification.setType(NotificationCode.LIKED_FOR_VIDEO.getCode());
        proxy.produce(notification);
    }

    @Override
    public void likeToCommentNotices(Integer myId, Integer authorId, Integer commentId) {
        Notification notification = Notification.builder()
                .messageId(UUID.randomUUID().toString())
                .userId(authorId)
                .targetId(myId)
                .tagId(commentId)
                .isRead(false)
                .messageId(UUID.randomUUID().toString())
                .type(NotificationCode.LIKED_FOR_COMMENT.getCode())
                .message(NotificationCode.LIKED_FOR_COMMENT.getDescription())
                .created(System.currentTimeMillis())
                .build();
        proxy.produce(notification);
    }

    @Override
    public void commentToCommentNotices(Integer myId, Integer authorId, Integer commentId) {
        Notification notification = Notification.builder()
                .messageId(UUID.randomUUID().toString())
                .userId(authorId)
                .targetId(myId)
                .tagId(commentId)
                .isRead(false)
                .created(System.currentTimeMillis())
                .type(NotificationCode.REPLY_FOR_COMMENT.getCode())
                .message(NotificationCode.REPLY_FOR_COMMENT.getDescription())
                .build();
        proxy.produce(notification);
    }

    @Override
    public void followToAuthorNotices(Integer myId, Integer authorId) {
        Notification notification= Notification.builder()
                .messageId(UUID.randomUUID().toString())
                .userId(authorId)
                .targetId(myId)
                .created(System.currentTimeMillis())
                .isRead(false)
                .type(NotificationCode.FOLLOWED_FOR.getCode())
                .message(NotificationCode.FOLLOWED_FOR.getDescription())
                .build();

        proxy.produce(notification);
    }

    @Override
    public void letterToOtherNotices(Integer myId, Integer otherId) {
        Notification notification = Notification.builder()
                .messageId(UUID.randomUUID().toString())
                .userId(otherId)
                .targetId(myId)
                .isRead(false)
                .created(System.currentTimeMillis())
                .type(NotificationCode.PRIVATE_LETTER_FOR.getCode())
                .message(NotificationCode.PRIVATE_LETTER_FOR.getDescription())
                .build();

        proxy.produce(notification);
    }

    @Override
    @Async
    public void newDevelopmentToFunNotices(Integer myId, Integer videoId) {
        List<Integer> fans=recordDao.findAllFanIdsByAuthorId(myId);
        if(fans==null || fans.isEmpty()) return;

        List<Notification> notificationList = new ArrayList<>();
        long time = System.currentTimeMillis();
        for(Integer fan : fans){
            Notification notification = Notification.builder()
                    .messageId(UUID.randomUUID().toString())
                    .userId(fan)
                    .targetId(myId)
                    .tagId(videoId)
                    .isRead(false)
                    .created(time)
                    .type(NotificationCode.DYNAMIC_PUBLISH_FOR.getCode())
                    .message(NotificationCode.DYNAMIC_PUBLISH_FOR.getDescription())
                    .build();
            notificationList.add(notification);
        }

        proxy.produce(notificationList);
    }

    @Override
    public void auditingStatusNotification(Integer userId, Integer videoId,boolean isPass) {
        if(userId==null || videoId==null) return;
        String message = NotificationCode.SERVER_BULLETIN.getDescription()+
                (isPass ? "你发布视频已经被审核通过"
                        : "你发布的视频审核不通过");

        Notification notification = Notification.builder()
                .messageId(UUID.randomUUID().toString())
                .userId(userId)
                .targetId(0)
                .tagId(videoId)
                .isRead(false)
                .created(System.currentTimeMillis())
                .type(NotificationCode.SERVER_BULLETIN.getCode())
                .message(message)
                .build();

        proxy.produce(notification);
    }
}
