package com.server.message.service.impl;

import com.server.dao.message.MessageLifeDao;
import com.server.message.entity.MessageLife;
import com.server.util.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScheduleHandleMessageLife implements DisposableBean, CommandLineRunner {
    @Autowired private MessageLifeDao messageLifeDao;
    @Autowired private RedisUtil redis;

    private final ConcurrentHashMap<String,Long> HISTORY_MESSAGE_LIFT = ChatWebSocketHandlerImpl.getHistoryMessageLift();
    private final Logger logger = LoggerFactory.getLogger(ScheduleHandleMessageLife.class);



    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanMessageCacheScheduled(){
        try{
            if(HISTORY_MESSAGE_LIFT.isEmpty()) return;

            Set<String> keys = new HashSet<>();
            for(Map.Entry<String,Long> map : HISTORY_MESSAGE_LIFT.entrySet()){
                if(map.getValue()<=System.currentTimeMillis()){
                    keys.add(map.getKey());
                }
            }

            redis.delete(keys);

        }catch (Exception e){
            logger.error("定时处理消息生命周期缓存失败 : {}",e.getMessage());
            throw e;
        }
    }


    @Override
    public void destroy() throws Exception {
        try {
            if (HISTORY_MESSAGE_LIFT.isEmpty()) return;
            List<MessageLife> lives = new ArrayList<>();
            for (Map.Entry<String, Long> map : HISTORY_MESSAGE_LIFT.entrySet()) {
                lives.add(new MessageLife(map.getKey(), map.getValue()));
            }

            messageLifeDao.batchInsert(lives);
        }catch (Exception e){
            logger.error("批量持久化消息生命周期失败 : {}",e.getMessage());
        }
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try{
            List<MessageLife> lives = messageLifeDao.findAll();
            if(lives==null || lives.isEmpty()) return;

            for(MessageLife life : lives){
                HISTORY_MESSAGE_LIFT.put(life.getRoomId(),life.getExpire());
            }

            messageLifeDao.batchDelete();
        }catch (Exception e){
            logger.error("初始化消息生命周期缓存失败 : {}",e.getMessage());
            throw e;
        }
    }
}
