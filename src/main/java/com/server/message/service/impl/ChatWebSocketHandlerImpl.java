package com.server.message.service.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.dao.message.MessageDao;
import com.server.entity.constant.RedisKeyConstant;
import com.server.entity.constant.WebConstant;
import com.server.message.dto.request.SendMessageRequest;
import com.server.message.entity.Message;
import com.server.message.service.ChatWebSocketHandler;
import com.server.push.service.NotificationService;
import com.server.util.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatWebSocketHandlerImpl implements ChatWebSocketHandler {

    @Autowired private RedisUtil redis;
    @Autowired private MessageDao messageDao;
    @Autowired private NotificationService notificationService;

    private final ObjectMapper mapper=new ObjectMapper();
    private static final int SEND_TIME_LIMIT=1000;
    private static final int BUFFER_SIZE_LIMIT=1024*1024;
    private static final int LIMIT_MESSAGES_SIZE=30;
    private static final int MAX_HISTORY_MESSAGE_SIZE=90;
    private static final long HISTORY_MESSAGE_LIFT_TIME=RedisKeyConstant.RANK_CACHE_LIFE_CYCLE;
    private final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandlerImpl.class);

    private static final ConcurrentHashMap<Integer,ConcurrentWebSocketSessionDecorator> onlineUsers=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String,Long> HISTORY_MESSAGE_LIFT=new ConcurrentHashMap<>();

    private String HISTORY_MESSAGES_LIST_KEY(Integer userId,Integer targetId){
        return userId<targetId
                ? RedisKeyConstant.HISTORY_MESSAGES_LIST_KEY+userId+":"+targetId
                : RedisKeyConstant.HISTORY_MESSAGES_LIST_KEY+targetId+":"+userId;
    }

    private String forRoom(Integer userId,Integer targetId){
        return userId<targetId ? userId+":"+targetId
                :targetId+":"+userId;
    }

    private List<String> serializationForMessages(List<Message> messages){
        List<String> messageStr=new ArrayList<>();
        try{
            for(Message message : messages){
                messageStr.add(mapper.writeValueAsString(message));
            }
            return messageStr;
        }catch (JacksonException e){
            logger.error("serializationForMessages fail reason is {}",e.getMessage());
            return null;
        }
    }

    /**
     * 当缓存满了 推一位 再插队
     * @param userId //用户id
     * @param targetId //好友id
     * @param message 序列化后的Message
     */
    private void setHistoryMessageOnCache(Integer userId,Integer targetId,String message){
        String key =HISTORY_MESSAGES_LIST_KEY(userId,targetId);
        Long len= redis.lLen(key);
        if(len==null || len < MAX_HISTORY_MESSAGE_SIZE) redis.rPush(key,message);
        else {
            redis.lPop(key);
            redis.rPush(key,message);
        }
    }


    /**
     * 在DB查询历史消息 当 cache 为空 || (cache满 && 需要数>cache数-offset) 才会在db查询
     *
     * @param userId      int
     * @param targetId    int
     * @param lastCreated long
     * @return list<Message>
     */
    private List<Message> findHistoryMessageOnDb(Integer userId, Integer targetId, Long lastCreated){
        if(lastCreated==null) return null;
        String room=forRoom(userId,targetId);
        return messageDao.findMessageByLastCreated(room,lastCreated, ChatWebSocketHandlerImpl.LIMIT_MESSAGES_SIZE);
    }

    /**
     * 在cache和db 查询为null 将 null 进入队列 ;若查询cache只有一位且为 null 则return null
     *
     * @param userId   用户id
     * @param targetId 目标id
     * @param offset   定位
     * @return List<Message> || null
     */
    private List<Message> findHistoryMessageOnCache(Integer userId, Integer targetId,int offset){
        String key = HISTORY_MESSAGES_LIST_KEY(userId,targetId);
        String room=forRoom(userId,targetId);
        List<Object> messageObj= redis.lRange(key,offset,offset+ ChatWebSocketHandlerImpl.LIMIT_MESSAGES_SIZE);
        if(messageObj!=null && messageObj.size()==1 && RedisKeyConstant.NULL.equals(messageObj.get(0).toString()))
            return null;

        if(offset==0 && (messageObj==null || messageObj.isEmpty())){
            List<Message> messages= messageDao.findMessageForCache(room,0,MAX_HISTORY_MESSAGE_SIZE);
            redis.rPushAll(key,
                    messages!=null && !messages.isEmpty()
                            ? serializationForMessages(messages)
                            : Collections.singletonList(RedisKeyConstant.NULL)
            );
            HISTORY_MESSAGE_LIFT.put(key,System.currentTimeMillis()+HISTORY_MESSAGE_LIFT_TIME);
            return messages;
        }

        List<Message> messages = new ArrayList<>();
        try {
            if (messageObj != null) {
                for (Object object : messageObj) {
                    messages.add(mapper.readValue(object.toString(),mapper.constructType(Message.class)));
                }
            }
            return messages;
        }catch (JacksonException e){
            logger.error("findHistoryMessageOnCache of deserialization fail reason:{}",e.getMessage());
            return null;
        }
    }

    /**
     * 
     * @param userId 用户id
     * @param targetId 好友id
     * @param lastCreated 上一条信息时间
     * @param offset int
     * @return List<Message>
     */
    @Override
    public List<Message> findHistoryMessage(Integer userId, Integer targetId, Long lastCreated,int offset){
        return offset>MAX_HISTORY_MESSAGE_SIZE ? findHistoryMessageOnDb(userId,targetId,lastCreated)
                : findHistoryMessageOnCache(userId,targetId,offset);
    }

    @Override
    public void produceMessage(Message message) {

    }

    @Override
    public void produceMessage(List<Message> messages) {

    }

    private void sendMessage(ConcurrentWebSocketSessionDecorator decorator,String data) throws IOException {
        decorator.sendMessage(new TextMessage(data));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            Integer userId = Integer.parseInt(session.getAttributes().get(WebConstant.WEBSOCKET_USER_ID).toString());
            ConcurrentWebSocketSessionDecorator decorator =
                    onlineUsers.putIfAbsent(userId,new ConcurrentWebSocketSessionDecorator(session,SEND_TIME_LIMIT,BUFFER_SIZE_LIMIT));

            if(decorator!=null){
                session.close(CloseStatus.NOT_ACCEPTABLE );
            }

        }catch (Exception e){
            logger.error("join initialization fail the reason is {}",e.getMessage());
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private SendMessageRequest deserialization(String data){
        try{
            SendMessageRequest request= mapper.readValue(data,mapper.constructType(SendMessageRequest.class));

            return request.getMessage()!=null && !request.getMessage().isEmpty() && request.getTarget_id()!=null
                    ? request
                    : null;
        } catch (JacksonException e){
            logger.error("deserialization fail reason:{}",e.getMessage());
            return null;
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            if (message instanceof TextMessage) {
                Integer userId = Integer.parseInt(session.getAttributes().get(WebConstant.WEBSOCKET_USER_ID).toString());
                String data = message.getPayload().toString();
                if (onlineUsers.containsKey(userId) && !data.isEmpty()) {
                    SendMessageRequest request = deserialization(data);
                    if (request == null) return;

                    Message newMessage = new Message(userId, request.getTarget_id(), request.getMessage());
                    try {
                        String MessageJson = mapper.writeValueAsString(newMessage);
                        ConcurrentWebSocketSessionDecorator decorator = onlineUsers.get(request.getTarget_id());
                        if(decorator!=null){
                            sendMessage(decorator, MessageJson);
                        }else{
                            notificationService.letterToOtherNotices(userId, request.getTarget_id());
                        }
                        redis.rPush(RedisKeyConstant.INSERT_MSG_FOR_MESSAGE_LIST_KEY,MessageJson);
                        setHistoryMessageOnCache(userId,newMessage.getTarget_id(),MessageJson);

                    } catch (JacksonException e) {
                        logger.error("serialization fail reason:{}", e.getMessage());
                    }
                }
            }
        }catch (Exception e){
            logger.error("fail reason:{}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Integer userId = Integer.parseInt(session.getAttributes().get(WebConstant.WEBSOCKET_USER_ID).toString());
        logger.error("user :{},transport ing message have fail ,reason is {}",userId,exception.getMessage());
        session.close();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Integer userId = Integer.parseInt(session.getAttributes().get(WebConstant.WEBSOCKET_USER_ID).toString());
        onlineUsers.remove(userId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }


    /**
     *历史消息缓存设计 :
     * 所有消息缓存在 队列当中 设定每3天清理一次缓存 ； 使用内存缓存和关机持久化进行存储历史消息有效生命周期 进行定期清理
     */
    public static ConcurrentHashMap<String,Long> getHistoryMessageLift(){
        return HISTORY_MESSAGE_LIFT;
    }
}
