package com.server.schedule;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.dao.message.MessageDao;
import com.server.dao.record.RecordDao;
import com.server.dao.stats.StatsDao;
import com.server.entity.cache.record.RecordUpdate;
import com.server.entity.cache.stats.StatsUpdateTask;
import com.server.entity.constant.RedisKeyConstant;
import com.server.dao.notification.NotificationDao;
import com.server.message.entity.Message;
import com.server.message.service.impl.ChatWebSocketHandlerImpl;
import com.server.push.entity.Notification;
import com.server.push.enums.NotificationCode;
import com.server.push.handle.NotificationHandlerImpl;
import com.server.push.service.impl.NotificationServiceImpl;
import com.server.util.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class ScheduleCenter {

    @Autowired
    private RedisUtil redis;

    @Autowired
    private StatsDao statsDao;

    @Autowired
    private RecordDao recordDao;

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private MessageDao messageDao;

    private static final long CLEAN_UPDATE_CACHE_SPACED=85L*1000;
    private static final long INSERT_NOTIFICATION_SPACED=45L*1000;
    private static final long INSERT_MESSAGES_SPACED=60L*1000;
    private final Logger logger= LoggerFactory.getLogger(ScheduleCenter.class);
    private final ObjectMapper MAPPER=new ObjectMapper();


    public static Set<String> updateKeys=new HashSet<>();

    static {

        updateKeys.add(RedisKeyConstant.VIDEO_STATS_UPDATE_KEY);
        updateKeys.add(RedisKeyConstant.USER_STATS_UPDATE_KEY);
        updateKeys.add(RedisKeyConstant.UPDATE_FAVORITE_KEY);
        updateKeys.add(RedisKeyConstant.UPDATE_COIN_KEY);
        updateKeys.add(RedisKeyConstant.UPDATE_LIKE_HASH_KEY);
        updateKeys.add(RedisKeyConstant.UPDATE_USER_RELATION_KEY);
    }

    private String HISTORY_MESSAGE_LIST_KEY(String userId,String type){
        return RedisKeyConstant.HISTORY_MESSAGE_LIST_KEY+userId+":"+type;
    }


    private Map<String,List<StatsUpdateTask>> columnCategorized(Map<Object,Object> stats){
        if(stats==null || stats.isEmpty()) return null;
        Map<String,List<StatsUpdateTask>> tasks =new HashMap<>();

        for(Map.Entry<Object,Object> hash : stats.entrySet()){
            String column=hash.getKey().toString();
            try {
                int count = Integer.parseInt(hash.getValue().toString());
                String[] members= column.split(":");//
                if(members.length!=2) continue;

                StatsUpdateTask task=new StatsUpdateTask(Integer.parseInt(members[1]),members[0],count);

                tasks.computeIfAbsent(members[0],k->new ArrayList<>()).add(task);

            }catch (Exception e){
                continue;
            }
        }

        return tasks;
    }

    private void updateInDbByStats(Map<String,List<StatsUpdateTask>> tasks,boolean isUserStats){
        if(tasks==null || tasks.isEmpty()) return;

        for(Map.Entry<String,List<StatsUpdateTask>> task : tasks.entrySet()){
            String column=task.getKey();
            List<StatsUpdateTask> statsUpdateTasks=task.getValue();
            if(statsUpdateTasks.isEmpty()) continue;

            for(int i=0;i<statsUpdateTasks.size();i+=500){
                int end=Math.min(i+500,statsUpdateTasks.size());
                try {
                    if (isUserStats) statsDao.batchUpdateUserStats(column, statsUpdateTasks.subList(i, end));
                    else statsDao.batchUpdateVideoStats(column, statsUpdateTasks.subList(i, end));
                }catch (Exception e){
                    logger.error("updateInDbByStats fail reason is {}",e.getMessage(),e);
                }
            }
        }
    }

    private void handleStatsUpdate(Map<Object,Object> userStatsData,Map<Object,Object> videoStatsData){

        Map<String,List<StatsUpdateTask>> userStatsUpdates=this.columnCategorized(userStatsData);
        Map<String,List<StatsUpdateTask>> videoStatsUpdates=this.columnCategorized(videoStatsData);

        updateInDbByStats(userStatsUpdates,true);
        updateInDbByStats(videoStatsUpdates,false);
    }


    private List<RecordUpdate> transformByRecord(Map<Object,Object> records){
        if(records==null || records.isEmpty()) return null;
        List<RecordUpdate> recordUpdates =new ArrayList<>();

        for(Map.Entry<Object,Object> record : records.entrySet()){
            String field=(String)record.getKey();
            boolean value=RedisKeyConstant.IS.equals(record.getValue().toString());

            try{
                String[] fields=field.split(":");
                recordUpdates.add(new RecordUpdate(Integer.parseInt(fields[0]),Integer.parseInt(fields[1]),value));
            }catch (Exception e){
                continue;
            }
        }

        return recordUpdates;
    }

    private List<RecordUpdate> transformByRelation(Map<Object,Object> relation){
        if(relation==null || relation.isEmpty()) return null;
        List<RecordUpdate> relationUpdates =new ArrayList<>();

        for(Map.Entry<Object,Object> record : relation.entrySet()){
            String field=(String) record.getKey();
            String value =(String) record.getValue();

            try{
                String[] fields= field.split(":");
                Boolean type= RedisKeyConstant.NULL_RECORD.equals(value) ? null : RedisKeyConstant.IS.equals(value);

                relationUpdates.add(new RecordUpdate(Integer.parseInt(fields[0]),Integer.parseInt(fields[1]),type));
            }catch (Exception e){
                continue;
            }
        }

        return relationUpdates;
    }

    private String transformForTableName(String key){
        return key.split(":")[0];
    }

    private void updateRecord(String table,List<RecordUpdate> records,boolean isRecord){
        if(records==null || records.isEmpty()) return;

        for(int i=0;i<records.size();i+=500){
            int end=Math.min(i+500,records.size());
            try {
                if (isRecord) this.recordDao.batchUpdateRecord(records.subList(i, end), transformForTableName(table));
                else this.recordDao.batchUpdateRelation(records.subList(i, end));
            }catch (Exception e){
                logger.error("updateRecord fail reason is {}",e.getMessage(),e);
            }
        }
    }


    private void  handleRecord(Map<Object,Object> coinUpdate,Map<Object,Object> favUpdate,
                               Map<Object,Object> likeUpdate,Map<Object,Object> relationUpdate){

        List<RecordUpdate> coinRecordList=this.transformByRecord(coinUpdate);
        List<RecordUpdate> favRecordList=this.transformByRecord(favUpdate);
        List<RecordUpdate> likeRecordList=this.transformByRecord(likeUpdate);
        List<RecordUpdate> relationList=this.transformByRelation(relationUpdate);

        this.updateRecord(RedisKeyConstant.COIN_RECORD_HASH_KEY,coinRecordList,true);
        this.updateRecord(RedisKeyConstant.FAVORITE_RECORD_HASH_KEY,favRecordList,true);
        this.updateRecord(RedisKeyConstant.LIKE_HASH_KEY,likeRecordList,true);
        this.updateRecord(RedisKeyConstant.USER_RELATION_HASH_KEY,relationList,false);
    }


    @SuppressWarnings("all")
    private Map<Object,Object> AtomicityGetData(String key){
        try {
            RedisTemplate<String, Object> redisTemplate = redis.getRedisTemplate();
            SessionCallback sessionCallback = new SessionCallback() {
                @Override
                public Object execute(RedisOperations redisOperations) throws DataAccessException {
                    redisOperations.multi();
                    redisOperations.opsForHash().entries(key);
                    redisOperations.delete(key);
                    return redisOperations.exec();
                }
            };

            Object execute = redisTemplate.execute(sessionCallback);
            if (execute == null) return null;
            List<Object> executes = (List<Object>) execute;
            if (executes.isEmpty()) return null;

            return (Map<Object, Object>) executes.get(0);
        }catch (Exception e){
            logger.error("AtomicityGetData(String key) 方法发生错误 : {}",e.getMessage(),e);
            return null;
        }
    }

    @Scheduled(fixedRate = CLEAN_UPDATE_CACHE_SPACED)
    public void updateAndCleanCache(){

        Map<Object,Object> userStatsData=AtomicityGetData(RedisKeyConstant.USER_STATS_UPDATE_KEY);
        Map<Object,Object> videoStatsData=AtomicityGetData(RedisKeyConstant.VIDEO_STATS_UPDATE_KEY);
        Map<Object,Object> coinUpdate=AtomicityGetData(RedisKeyConstant.UPDATE_COIN_KEY);
        Map<Object,Object> favUpdate=AtomicityGetData(RedisKeyConstant.UPDATE_FAVORITE_KEY);
        Map<Object,Object> likeUpdate=AtomicityGetData(RedisKeyConstant.UPDATE_LIKE_HASH_KEY);
        Map<Object,Object> relationUpdate=AtomicityGetData(RedisKeyConstant.UPDATE_USER_RELATION_KEY);

        try{
            this.handleStatsUpdate(userStatsData,videoStatsData);
        }catch (Exception e) {
            logger.error("updateAndCleanCache 删除缓stats存缓存失败 : {}", e.getMessage(), e);
        }
        try{
            this.handleRecord(coinUpdate,favUpdate, likeUpdate,relationUpdate);
        }catch (Exception e) {
            logger.error("updateAndCleanCache 删除record缓存缓存失败 : {}", e.getMessage(), e);
        }

    }


    private void updateVideoRank(){
        try {

            Map<Object, Object> lifts = redis.hGetAll(RedisKeyConstant.VIDEO_RANK_LIFE_HASH_KEY);
            if (!lifts.isEmpty()) {

                long now = System.currentTimeMillis();
                Set<String> videoIds = new HashSet<>();
                for (Map.Entry<Object, Object> lift : lifts.entrySet()) {
                    String videoId = lift.getKey().toString();
                    long time = Long.parseLong(lift.getValue().toString());
                    if (time - now > RedisKeyConstant.RANK_CACHE_LIFE_CYCLE) {
                        videoIds.add(videoId);
                    }
                }
                redis.zRem(RedisKeyConstant.VIDEO_RANKING_KEY, String.join(",", videoIds));
                redis.hDel(RedisKeyConstant.VIDEO_RANK_LIFE_HASH_KEY, String.join(",", videoIds));
            }
        }catch (Exception e) {
            logger.error("updateVideoRankSpaced 删除缓存缓存失败 : {}", e.getMessage(), e);
        }
    }

    private void cleanNotificationCache(){
        try {
            ConcurrentHashMap<String, Long> userActivity = NotificationHandlerImpl.getUserActivity();
            if (!userActivity.isEmpty()) {
                Set<String> unreadIds = new HashSet<>();
                Set<String> historyIds = new HashSet<>();
                Set<Map.Entry<String, Long>> userActivities = userActivity.entrySet();
                Set<String> keys = new HashSet<>();
                Integer[] codes = NotificationCode.getCodes();

                for (Map.Entry<String, Long> activity : userActivities) {
                    //提取出两个缓存的keys
                    Long expired = activity.getValue();
                    if (expired != null && expired <= System.currentTimeMillis()) {
                        String key = activity.getKey();
                        keys.add(key);
                        unreadIds.add(RedisKeyConstant.UNREAD_MESSAGE_LIST_KEY + key);

                        for (Integer code : codes) {
                            historyIds.add(HISTORY_MESSAGE_LIST_KEY(key, code.toString()));
                        }
                    }
                }

                redis.delete(unreadIds);
                redis.delete(historyIds);
                for(String key : keys){
                    userActivity.remove(key);
                }
            }
        } catch (Exception e) {
            logger.error("updateVideoRankSpaced 删除缓存缓存失败 : {}", e.getMessage(), e);
        }
    }

    private void cleanMessageCache(){
        try{
            ConcurrentHashMap<String, Long> historyMessageLiftForKeys= ChatWebSocketHandlerImpl.getHistoryMessageLift();
            if(historyMessageLiftForKeys.isEmpty()) return;

            Set<String> keys = new HashSet<>();
            for(Map.Entry<String,Long> map : historyMessageLiftForKeys.entrySet()){
                if(map.getValue()<=System.currentTimeMillis()){
                    keys.add(map.getKey());
                }
            }

            redis.delete(keys);

        }catch (Exception e){

        }
    }

    /**
     * 凌晨0点更新
     */
    @Scheduled(cron = "0 0 0 * * ?") //每天执行一次
    public void updateVideoRankSpaced() {
        updateVideoRank();

    }

    /**
     * 凌晨0点半更新
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void cleanNotificationCacheScheduled(){
        cleanNotificationCache();
    }

    /**
     * 凌晨1点更新
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanMessageCacheScheduled(){
        cleanMessageCache();
    }

    @Scheduled(fixedRate = INSERT_NOTIFICATION_SPACED)
    public void insertNotificationSpaced(){
        try {
            List<String> notifications = redis.getRedisTemplate().execute(
                    new SessionCallback<List<String>>() {
                        @Override
                        public List<String> execute(RedisOperations operations) throws DataAccessException {
                            operations.multi();
                            operations.opsForList().range(RedisKeyConstant.INSERT_MESSAGE_LIST_KEY, 0, -1);
                            operations.delete(RedisKeyConstant.INSERT_MESSAGE_LIST_KEY);
                            List<Object> results = operations.exec();

                            if (!results.isEmpty()) {
                                return (List<String>) results.get(0);
                            }
                            return Collections.emptyList();
                        }
                    }
            );

            if(notifications.isEmpty()) return;

            List<Notification> notificationList =new ArrayList<>();
            for(String msg:notifications){
                notificationList.add(MAPPER.readValue(msg,MAPPER.constructType(Notification.class)));
            }

            for(int i=0;i<notifications.size();i+=500){
                int end =Math.min(i+500,notifications.size());
                notificationDao.batchInsert(notificationList.subList(i,end));
            }
        }catch (Exception e){
            logger.error("批量插入notification 失败 : {}",e.getMessage(),e);
        }
    }


    private void updateNotificationStatusOnDb(){
        try{
            List<String> ids=redis.getRedisTemplate().execute(new SessionCallback<List<String>>() {
                @Override
                public List<String> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    operations.opsForList().range(RedisKeyConstant.MESSAGE_UPDATE_LIST_KEY,0,-1);
                    operations.delete(RedisKeyConstant.MESSAGE_UPDATE_LIST_KEY);
                    List<Object> result = (List<Object>) operations.exec();
                    return result==null ? null : (List<String>) result.get(0);
                }
            });

            if(ids.isEmpty()) return;

            List<String> IDS=new ArrayList<>();
            for(String id : ids){
                try {
                    if(id!=null)
                        IDS.add(id);
                }catch (Exception e){
                    continue;
                }
            }

            if(!IDS.isEmpty()) {
                for (int i = 0; i < IDS.size(); i += 500) {
                    int end = Math.min(i + 500, IDS.size());
                    notificationDao.batchUpdateStatus(IDS.subList(i, end));
                }
            }

        }catch (Exception e){
            logger.error("批量更新notification 失败 : {}",e.getMessage(),e);
        }
    }

    private void updateNotificationStatusOnCache(){
        try{
            ConcurrentLinkedQueue<String> messageIds= NotificationServiceImpl.UNREAD_NOTIFICATIONS;
            if(!messageIds.isEmpty()){
                List<String> updateIds = new ArrayList<>();
                String id=messageIds.poll();
                while (id!=null){
                    updateIds.add(id);
                    id=messageIds.poll();
                }

                for (int i = 0; i < messageIds.size(); i += 500) {
                    int end = Math.min(i + 500, messageIds.size());
                    notificationDao.batchUpdateStatus(updateIds.subList(i, end));
                }
            }

        }catch (Exception e){
            logger.error("批量更新notification 失败 : {}",e.getMessage(),e);
        }
    }

    @Scheduled(fixedRate = CLEAN_UPDATE_CACHE_SPACED)
    public void updateNotificationSpaced(){
        updateNotificationStatusOnDb();
        updateNotificationStatusOnCache();
    }


    @Scheduled(fixedRate = INSERT_MESSAGES_SPACED)
    public void insertMessagesSpaced(){
        List<String> messages;
        try {
            messages = redis.getRedisTemplate().execute(new SessionCallback<List<String>>() {
                @Override
                public List<String> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    operations.opsForList().range(RedisKeyConstant.INSERT_MSG_FOR_MESSAGE_LIST_KEY, 0, -1);
                    operations.delete(RedisKeyConstant.INSERT_MSG_FOR_MESSAGE_LIST_KEY);
                    List<Object> result = operations.exec();
                    return result == null ? null : (List<String>) result.get(0);
                }
            });
        }catch (Exception e){
            logger.error("get messages data of redis for fail,the reason is {}",e.getMessage());
            return;
        }

        try{
            if(messages==null || messages.isEmpty()) return;

            List<Message> messageList =new ArrayList<>();
            for(String str : messages){
                messageList.add(MAPPER.readValue(str,MAPPER.constructType(Message.class)));
            }

            messageDao.batchInsertMessage(messageList);

        }catch (JacksonException jacksonException){
            logger.error("deserialize of Message for fail ,the reason is {}",jacksonException.getMessage());
        }catch (Exception e){
            logger.error("batchInsertMessage fail ,the reason is {}",e.getMessage());
        }
    }
}
