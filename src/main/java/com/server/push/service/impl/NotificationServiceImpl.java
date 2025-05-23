package com.server.push.service.impl;

import com.server.comment.entity.Comment;
import com.server.dao.notification.NotificationDao;
import com.server.dao.record.RecordDao;
import com.server.dto.response.user.UserResponse;
import com.server.dto.response.video.VideoDataResponse;
import com.server.push.dto.request.NotificationForVideoResponse;
import com.server.push.dto.response.HistoryNotificationResponse;
import com.server.push.entity.Notification;
import com.server.push.enums.NotificationCode;
import com.server.push.handle.NotificationHandleProxy;
import com.server.push.service.NotificationService;
import com.server.comment.server.CommentService;
import com.server.service.userservice.UserDataService;
import com.server.service.videoservice.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired private NotificationHandleProxy proxy;
    @Autowired private RecordDao recordDao;
    @Autowired private VideoService videoService;
    @Autowired private UserDataService userDataService;
    @Autowired private NotificationDao notificationDao;
    @Autowired private CommentService commentService;


    public static final ConcurrentLinkedQueue<String> UNREAD_NOTIFICATIONS=new ConcurrentLinkedQueue<>();

    private Map<String,List<Notification>> classifyForTagId(List<Notification> notifications){
        if(notifications==null || notifications.isEmpty()) return null;
        Map<String,List<Notification>> classify = new HashMap<>();

        for(Notification notification : notifications){
            classify.computeIfAbsent(notification.getTag_id().toString(),k->new ArrayList<>()).add(notification);
        }

        return classify;
    }

    @Override
    public List<NotificationForVideoResponse> getLikeVideoNotification(Integer userId, int offset)
            throws InterruptedException {
        List<Notification> notificationList= proxy.findHistoryNotifications(userId,NotificationCode.LIKED_FOR_VIDEO,offset);
        Map<String,List<Notification>> notificationMapList= classifyForTagId(notificationList);

        if(notificationMapList==null) return null;

        List<NotificationForVideoResponse> notificationForVideoResponses = new ArrayList<>();
        for(Map.Entry<String,List<Notification>> notificationEntry : notificationMapList.entrySet()){
            Integer videoId = Integer.parseInt(notificationEntry.getKey());
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


    public static class NotificationReplyResponse{
        private Comment comment;
        private UserResponse user;
        private Notification notification;

        public Notification getNotification() {
            return notification;
        }

        public Comment getComment() {
            return comment;
        }

        public UserResponse getUser() {
            return user;
        }

        public void setComment(Comment comment) {
            this.comment = comment;
        }

        public void setUser(UserResponse user) {
            this.user = user;
        }

        public void setNotification(Notification notification) {
            this.notification = notification;
        }

        public NotificationReplyResponse(){}
        public NotificationReplyResponse(Comment comment,UserResponse user,Notification notification){this.comment=comment;this.user=user;this.notification=notification;}
    }

    private List<NotificationReplyResponse> getCommentInteractionNotification(Integer userId,int offset,NotificationCode code)
            throws InterruptedException {
        List<Notification> notificationList = proxy.findHistoryNotifications(userId,code,offset);

        List<NotificationReplyResponse> replyResponses = new ArrayList<>();
        for(Notification notification : notificationList){
            Comment comment = commentService.getCommentOnCache((String) notification.getTag_id());
            UserResponse userResponse = userDataService.getUserResponseData(notification.getTarget_id());
            replyResponses.add(new NotificationReplyResponse(comment,userResponse,notification));
        }

        return replyResponses;

//        Map<String,List<Notification>> notificationCommentMap=classifyForTagId(notificationList);
//
//        if(notificationList==null) return null;
//
//        List<NotificationForComment> notificationForCommentList = new ArrayList<>();
//        for(Map.Entry<String,List<Notification>> notificationEntry : notificationCommentMap.entrySet()){
//            String commentId = notificationEntry.getKey();
//            List<Notification> notifications = notificationEntry.getValue();
//
//            Comment comment= commentService.getCommentOnCache(commentId);
//            if(comment==null) continue;
//
//            VideoDataResponse video = videoService.getVideoResponseData(comment.getVideo_id(),null);
//            NotificationForComment notificationForComment= new NotificationForComment(comment,video);
//
//            List<HistoryNotificationResponse<Integer>> historyNotificationResponses = new ArrayList<>();
//            for(Notification notification : notifications){
//                if(notification!=null){
//                    UserResponse user = userDataService.getUserResponseData(notification.getTarget_id());
//                    historyNotificationResponses.add(new HistoryNotificationResponse<>(notification,user));
//
//                    if(!notification.getIs_read()){
//                        UNREAD_NOTIFICATIONS.offer(notification.getMessage_id());
//                    }
//                }
//            }
//
//            notificationForComment.setNotificationWithUser(historyNotificationResponses);
//            notificationForCommentList.add(notificationForComment);
//        }
//
//        return notificationForCommentList;
    }

    @Override
    public List<NotificationReplyResponse> getLikeCommentNotification(Integer userId, int offset) throws InterruptedException {
        return getCommentInteractionNotification(userId,offset,NotificationCode.LIKED_FOR_COMMENT);
    }

    @Override
    public List<NotificationReplyResponse> getReplyCommentNotification(Integer userId, int offset) throws InterruptedException {
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
    public void deleteNotifications(Integer userId, NotificationCode type) {
        proxy.deleteMessage(userId,type);
    }

    @Override
    public void deleteNotifications(int userId, int targetId) {
        proxy.deleteMessage(userId,targetId);
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
    public void likeToCommentNotices(Integer myId, Integer authorId, String commentId) {
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
    public void commentToCommentNotices(Integer myId, Integer authorId, String commentId) {
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
    public void auditingStatusNotification(Integer userId, Integer videoId, boolean isPass, @Nullable String title) {
        if(userId==null || videoId==null) return;
        String message = NotificationCode.SERVER_BULLETIN.getDescription()+
                (isPass ? "你发布视频已经被审核通过"
                        : "你发布的视频"+title+"审核不通过");

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
