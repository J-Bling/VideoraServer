package com.server.push.handle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.entity.constant.RedisKeyConstant;
import com.server.entity.constant.WebConstant;
import com.server.push.dto.response.NotificationResponse;
import com.server.push.enums.NotificationCode;
import com.server.enums.WSStatusCode;
import com.server.dao.notification.NotificationDao;
import com.server.push.dto.request.MessageRequestOfRead;
import com.server.push.dto.response.MessageErrorCode;
import com.server.push.dto.response.WsMessage;
import com.server.push.entity.Notification;
import com.server.service.stats.UserStatsService;
import com.server.util.redis.RedisUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationHandlerImpl implements NotificationHandler {
    @Autowired private RedisUtil redis;
    @Autowired private NotificationDao notificationDao;
    @Autowired private UserStatsService userStatsService;

    private static final ConcurrentHashMap<String, ConcurrentWebSocketSessionDecorator> onlineUsers= new ConcurrentHashMap<>();

    /**
        对于未读消息缓存 策略 是 以用户活跃度作为 需不需要继续向缓存新增未读消息缓存 依据
        当用户活跃度为0时把该用户的历史缓存 和未读消息缓存一并删除 以及不再缓存
     */
    @Getter
    private static final ConcurrentHashMap<String,Long> userActivity =new ConcurrentHashMap<>();
    private static final int SEND_TIME_LIMIT=1000;
    private static final int BUFFER_SIZE_LIMIT=1024*1024;

    private final ObjectMapper MAPPER=new ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(NotificationHandlerImpl.class);
    private String UNREAD_MESSAGE_LIST_KEY(String userId){
        return RedisKeyConstant.UNREAD_MESSAGE_LIST_KEY+userId;
    }
    private String HISTORY_MESSAGE_LIST_KEY(String userId,String type){
        return RedisKeyConstant.HISTORY_MESSAGE_LIST_KEY+userId+":"+type;
    }

    private static final int MIN_PRODUCE_SIZE=300;
    private static final int MAX_NOTIFICATION_SIZE=99;
    private static final int MAX_LIMIT=20;
    /**
        需要按时清理 INSERT_MESSAGE_LIST_KEY , MESSAGE_UPDATE_LIST_KEY ,
        HISTORY_MESSAGE_HASH_KEY 缓存 限定长度为99

        插入等待队列清理频率要大于 更新等待队列
     */
    private void preheat(String userId){
        //  缓存预热
    }

    /**
     *启动用户缓存 把未读消息,以及部份历史消息进行预热缓存
     */
    private String[] start(String userId){
        if(userId!=null) {
            Long expire = userActivity.get(userId);
            if (expire != null && System.currentTimeMillis() < expire) {

                if(redis.lLen(UNREAD_MESSAGE_LIST_KEY(userId))>0 ) {
                    List<Object> objectList= collectUnreadNotificationByCache(userId);
                    String[] messages = new String[objectList.size()];
                    int i=0;
                    for(Object object : objectList){
                        messages[i++] = object.toString();
                    }
                    return messages;
                }

                List<Notification> notificationList =
                        notificationDao.findUnreadMessagesByUserId(Integer.parseInt(userId));
                String[] messages = new String[notificationList.size()];
                ObjectMapper mapper = new ObjectMapper();
                int i=0;
                try{
                    for(Notification notification : notificationList){
                        String msg = mapper.writeValueAsString(notification);
                        if(msg!=null) messages[i++]=msg;
                    }
                    insertMessageOnUnreadCache(userId,messages);

                }catch (JsonProcessingException e){
                    logger.error("start 方法 json序列化失败 : {}",e.getMessage(),e);
                }
                preheat(userId);

                return messages;
            }
        }
        return null;
    }

    private List<Object> collectUnreadNotificationByCache(String userId){
        return redis.lRange(UNREAD_MESSAGE_LIST_KEY(userId),0,-1);
    }

    /**
        *操作session 对消息进行发送 , 并发使用session线程不安全
        *Param  session 当前session
        *Param  data 发送的数据
     */
    private void sendMessage(ConcurrentWebSocketSessionDecorator session , String data) throws IOException {
        if(session==null || data==null) return;
        session.sendMessage(WsMessage.transTextMessage(data));
    }

    private void updateMessagesStatusInQueue(List<String> messageIds){
        if(messageIds==null) return;
        String[] ids=messageIds.toArray(new String[0]);
        redis.rPush(RedisKeyConstant.MESSAGE_UPDATE_LIST_KEY,ids);
    }

    private String isContains(String id,List<Object> values){
        for(Object value : values){
            String msg = value.toString();
            if(msg.contains(id)) return msg;
        }
        return null;
    }

    private void cleanIsReadMessage(String userId,List<String> messageIds){
        if(messageIds==null || messageIds.isEmpty()) return;
        String key = UNREAD_MESSAGE_LIST_KEY(userId);
        List<Object> notifications = collectUnreadNotificationByCache(userId);
        if(notifications==null || notifications.isEmpty()) return;

        if(messageIds.size()==notifications.size()){
            redis.delete(key);
            insertMessageOnUnreadCache(userId,RedisKeyConstant.NULL);
        }
        else {
            for (String id : messageIds) {
                String vale = isContains(id, notifications);
                if (vale != null) {
                    redis.lRem(key, vale);
                }
            }
        }
    }

    private void insertMessageWaitQueue(String message){
        redis.rPush(RedisKeyConstant.INSERT_MESSAGE_LIST_KEY,message);
    }

    private void insertMessageOnHistoryCache(String userId,Integer type,String message){
        String key = HISTORY_MESSAGE_LIST_KEY(userId,type.toString());
        redis.rPush(key,message);
    }

    private void insertMessageOnHistoryCache(String userId,Integer type,List<Notification> notificationList){
        Long expire =userActivity.get(userId);
        if(expire!=null && expire>System.currentTimeMillis()){
            String key = HISTORY_MESSAGE_LIST_KEY(userId,type.toString());
            String[] data=new String[notificationList.size()];
            int i=0;
            try{
                for(Notification notification : notificationList){
                    data[i++] = MAPPER.writeValueAsString(notification);
                }
                redis.rPushAll(key,data);
            }catch (JsonProcessingException e){
                logger.error("insertMessageOnHistoryCache 方法 序列化失败 : {}",e.getMessage(),e);
            }
        }
    }

    private void insertMessageOnUnreadCache(String userId,String message){
        String key =UNREAD_MESSAGE_LIST_KEY(userId);
        redis.rPush(key,message);
    }

    private void insertMessageOnUnreadCache(String userId,List<Notification> message){
        String[] messages=new String[message.size()];
        try {
            ObjectMapper mapper = new ObjectMapper();
            int i=0;
            for (Notification notification : message) {
                messages[i++]=mapper.writeValueAsString(notification);
            }

            insertMessageOnUnreadCache(userId,messages);
        }catch (JsonProcessingException e){
            logger.error("insertMessageOnUnreadCache方法 序列化失败 : {}",e.getMessage(),e);
        }
    }

    private void insertMessageOnUnreadCache(String userId,String[] message){
        if(message==null || message.length<1) return;
        String key =UNREAD_MESSAGE_LIST_KEY(userId);
        redis.rPushAll(key,message);
    }

    @Override
    public void produceOneMessage(Notification notification){
        try{
            String userId = notification.getUser_id().toString();

            ObjectMapper mapper = new ObjectMapper();
            String data = mapper.writeValueAsString(notification);
            insertMessageWaitQueue(data);//进入插入等待队列

            Long expire = userActivity.get(userId);
            if(expire!=null && expire>System.currentTimeMillis()) {

                notification.setIs_read(true);
                String message = mapper.writeValueAsString(notification);

                insertMessageOnHistoryCache(userId, notification.getType(), message);//进入历史缓存
                insertMessageOnUnreadCache(notification.getUser_id().toString(), data);//进入未读缓存

            }
            ConcurrentWebSocketSessionDecorator session = onlineUsers.get(userId);
            if(session!=null){
                NotificationResponse response =new NotificationResponse(new String[]{data});
                sendMessage(session,mapper.writeValueAsString(response));
            }

        }catch (JsonProcessingException e){
            logger.error("produceOneMessage 序列化失败 原因 : {}",e.getMessage(),e);
        } catch (IOException e) {
            logger.error("produceOneMessage 发送消息失败 原因 : {}",e.getMessage(),e);
        }
    }

    private Map<Integer,Set<Notification>> categoryNotificationByUser(List<Notification> notificationList){
        Map<Integer,Set<Notification>> messages = new HashMap<>();
        for(Notification notification : notificationList){
            messages.computeIfAbsent(notification.getUser_id(),k->new HashSet<>()).add(notification);
        }
        return messages;
    }

    @Async
    private void batchProduceMessage(List<Notification> notificationList){
        List<String> notificationJsonStr=new ArrayList<>();
        Map<Integer,Set<Notification>> messages = categoryNotificationByUser(notificationList);

        try{
            ObjectMapper mapper =new ObjectMapper();
            for(Map.Entry<Integer,Set<Notification>> message : messages.entrySet()){
                String userId=message.getKey().toString();
                Set<Notification> notifications = message.getValue();

                Long expire=userActivity.get(userId);
                boolean isExpire=expire!=null && System.currentTimeMillis()<expire;

                String[] data = new String[notifications.size()];
                int i=0;

                for(Notification notification: notifications){
                    String msg= mapper.writeValueAsString(notification);
                    data[i++]=msg;
                    notificationJsonStr.add(msg);
                    if(isExpire){
                        notification.setIs_read(true);
                        insertMessageOnHistoryCache(userId,notification.getType(),msg);
                    }
                }

                if(isExpire){
                    insertMessageOnUnreadCache(userId,data);
                }

                ConcurrentWebSocketSessionDecorator session = onlineUsers.get(userId);
                NotificationResponse response =new NotificationResponse(data);
                sendMessage(session,mapper.writeValueAsString(response));
            }

            redis.rPushAll(RedisKeyConstant.INSERT_MESSAGE_LIST_KEY,notificationJsonStr);

        }catch (JsonProcessingException e){
            logger.error("batchProduceMessage 方法 序列化发生错误 : {}",e.getMessage(),e);
        } catch (IOException e) {
            logger.error("batchProduceMessage 方法 传输流发生错误 : {}",e.getMessage(),e);
        }

    }

    @Override
    public void produceOneMessage(List<Notification> notificationList){
        if(notificationList.size()<MIN_PRODUCE_SIZE){
            for(Notification notification : notificationList){
                produceOneMessage(notification);
            }
            return;
        }

        batchProduceMessage(notificationList);
    }

    @Override
    public List<Notification> findUnreadNotification(Integer userId){
        List<Object> notificationObjectList= redis.lRange(UNREAD_MESSAGE_LIST_KEY(userId.toString()),0,-1);
        if(notificationObjectList==null || notificationObjectList.isEmpty()){

            List<Notification> notifications=notificationDao.findUnreadMessagesByUserId(userId);

            if(notifications==null || notifications.isEmpty()) {
                insertMessageOnUnreadCache(userId.toString(),RedisKeyConstant.NULL);
                return null;
            }

            Long expire = userActivity.get(userId.toString());
            if(expire!=null && expire > System.currentTimeMillis()){
                insertMessageOnUnreadCache(userId.toString(),notifications);
            }
            return notifications;
        }

        if(notificationObjectList.size()==1 && RedisKeyConstant.NULL.equals(notificationObjectList.get(0).toString())){
            return null;
        }

        List<Notification> notifications = new ArrayList<>();
        for(Object notification : notificationObjectList){
            notifications.add((Notification) notification);
        }
        return notifications;
    }

    /**
     *查找历史通知 用户活跃度过期和 缓存的offset超过一定数量就查询数据库
     * @param offset 偏远量 消息不够就数据库查询
     */
    @Override
    public List<Notification> findHistoryNotification(Integer userId, NotificationCode type, int offset){
        String key = HISTORY_MESSAGE_LIST_KEY(userId.toString(),type.getCode().toString());
        List<Object> notifications = redis.lRange(key,offset,offset+MAX_LIMIT);
        if(notifications==null || notifications.isEmpty()){
            List<Notification> notificationList = notificationDao.findHistoryNotificationByType(userId,type.getCode(),offset,MAX_LIMIT);

            if(notificationList !=null && !notificationList.isEmpty() && offset<=MAX_NOTIFICATION_SIZE){
                this.insertMessageOnHistoryCache(userId.toString(), type.getCode(),notificationList);
            }

            return notificationList;
        }

        try {
            List<Notification> notificationList = new ArrayList<>();
            for (Object item : notifications) {
                notificationList.add(MAPPER.readValue((String) item,MAPPER.constructType(Notification.class)));
            }
            return notificationList;
        }catch (Exception e){
            logger.error("error : {}",e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteNotification(Integer userId, NotificationCode type) {
        String key = HISTORY_MESSAGE_LIST_KEY(userId.toString(),type.getCode().toString());
        redis.delete(key);
        notificationDao.deleteNotification(userId,type.getCode());
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception
    {
        //连接时调用
        String userId= session.getAttributes().get(WebConstant.WEBSOCKET_USER_ID).toString();

        userStatsService.recordOnline(userId);

        ConcurrentWebSocketSessionDecorator decorator =new ConcurrentWebSocketSessionDecorator(session,SEND_TIME_LIMIT,BUFFER_SIZE_LIMIT);
        ConcurrentWebSocketSessionDecorator sessionDecorator = onlineUsers.putIfAbsent(userId,decorator);

        if(sessionDecorator!=null){
            session.close(new CloseStatus(WSStatusCode.SINGLE_USERID_ALLOWED.getCode(),WSStatusCode.SINGLE_USERID_ALLOWED.getDescription()));
            return;
        }

        userActivity.put(userId,System.currentTimeMillis()+RedisKeyConstant.USER_ACTIVATE_FILE);//更新用户活跃度

        String[] message=start(userId);
        if(message!=null && message.length>0){
            NotificationResponse response= new NotificationResponse(message);

            try{
                String msg = MAPPER.writeValueAsString(response);
                sendMessage(decorator,msg);
            }catch (JsonProcessingException e){
                logger.error("在 afterConnectionEstablished 方法 json序列化失败 : {}",e.getMessage(),e);
            }
        }
    }


    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

        if (message instanceof BinaryMessage) {
            try {
                String data= MAPPER.writeValueAsString(new MessageErrorCode(WSStatusCode.NOT_ACCEPTABLE));
                session.sendMessage(WsMessage.transTextMessage(data));
            }catch (JsonProcessingException e){
                logger.error("序列化失败 : {}",e.getMessage(),e);
            }
            return;
        }
        String userId= session.getAttributes().get(WebConstant.WEBSOCKET_USER_ID).toString();
        ConcurrentWebSocketSessionDecorator decorator = onlineUsers.get(userId);
        String data= message.getPayload().toString();

        if(decorator!=null && !data.isEmpty()){
            try{
                MessageRequestOfRead readMessages= MAPPER.readValue(data,MAPPER.constructType(MessageRequestOfRead.class));
                if(readMessages==null) return;
                readMessages.collection();
                cleanIsReadMessage(userId,readMessages.getMessageIds());
                updateMessagesStatusInQueue(readMessages.getMessageIds());

            }catch (JsonProcessingException e){
                logger.error("反序列化失败 : {}",e.getMessage());
            }
        }

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        //处理来自底层ws消息传输的错误
        String userId= session.getAttributes().get(WebConstant.WEBSOCKET_USER_ID).toString();
        if(userId!=null){
            onlineUsers.remove(userId);
            userActivity.put(userId, System.currentTimeMillis() + RedisKeyConstant.USER_ACTIVATE_FILE);
        }
        session.close();
        logger.error("传输发生错误 userId:{}, sessionId:{}, 原因: {}",
                session.getAttributes().get(WebConstant.WEBSOCKET_USER_ID),
                session.getId(),
                exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        //在任一端关闭连接或者传输发送错误时调用 从技术上说session可能还在打开状态 但不建议在这里发送消息 要为有可能不成功
        String userId = session.getAttributes().get(WebConstant.WEBSOCKET_USER_ID).toString();
        if (userId != null) {
            onlineUsers.remove(userId);
            userActivity.put(userId, System.currentTimeMillis() + RedisKeyConstant.USER_ACTIVATE_FILE);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        //是否支持分片消息
        return false;
    }
}
