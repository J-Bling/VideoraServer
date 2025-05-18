package com.server.util.redis;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class RedisUtil {

    private final ObjectMapper objectMapper=new ObjectMapper();
    private final Logger logger= LoggerFactory.getLogger(RedisUtil.class);

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    public Boolean exists(String key){
        return redisTemplate.hasKey(key);
    }

    public void expire(String key,long time){
        redisTemplate.expire(key,time, TimeUnit.MILLISECONDS);
    }

    public void set(String key,String value){
        redisTemplate.opsForValue().set(key,value);
    }

    public void set(String key,String value,long expire){
        redisTemplate.opsForValue().set(key,value,expire,TimeUnit.MILLISECONDS);
    }

    public String get(String key){
        return (String) redisTemplate.opsForValue().get(key);
    }

    public Boolean setIfAbsent(String key,String value,long duration){
        return (Boolean) redisTemplate.opsForValue().setIfAbsent(key,value,duration,TimeUnit.MILLISECONDS);
    }

    public String getSet(String key,String value){
        return (String) redisTemplate.opsForValue().getAndSet(key,value);
    }

    public void mSet(Map<String,String> map){
        redisTemplate.opsForValue().multiSet(map);
    }

    public List<Object> mGet(Set<String> strings){
        return redisTemplate.opsForValue().multiGet(strings);
    }

    public void inCrBy(String key,long count){
        redisTemplate.opsForValue().increment(key,count);
    }

    public void deCrBy(String key,long count){
        redisTemplate.opsForValue().decrement(key,count);
    }

    public void hSet(String key,String field,Object value){
        redisTemplate.opsForHash().put(key,field,value);
    }


    public Object hGet(String key,String field){
        return redisTemplate.opsForHash().get(key,field);
    }

    public void hmSet(String key,Map<String,Object> map){
        redisTemplate.opsForHash().putAll(key,map);
    }

    public void hmSet(String key,Map<String,Object> map,long expire){
        redisTemplate.opsForHash().putAll(key,map);
        redisTemplate.expire(key,expire,TimeUnit.MILLISECONDS);
    }

    public List<Object> hmGet(String key, Set<Object> strings){
        return redisTemplate.opsForHash().multiGet(key,strings);
    }

    public Map<Object,Object> hGetAll(String key){
        //获取键和值
        return redisTemplate.opsForHash().entries(key);
    }

    @SuppressWarnings("all")
    public void hDel(String key,String... field){
        redisTemplate.opsForHash().delete(key,field);
    }

    public Long hLen(String key){
        return redisTemplate.opsForHash().size(key);
    }

    public Boolean hExists(String key,String field){
        return redisTemplate.opsForHash().hasKey(key,field);
    }

    public Set<Object> hKeys(String key){
        return redisTemplate.opsForHash().keys(key);
    }

    public List<Object> hVals(String key){
        return redisTemplate.opsForHash().values(key);
    }

    public Long hInCrBy(String key,String field,long count){
        return redisTemplate.opsForHash().increment(key,field,count);
    }

    public void sAdd(String key,String member){
        redisTemplate.opsForSet().add(key,member);
    }

    public Set<Object> sMembers(String key){
        return redisTemplate.opsForSet().members(key);
    }

    public Boolean sisMember(String key,String member){
        return redisTemplate.opsForSet().isMember(key,member);
    }

    public Long sCard(String key){
        return redisTemplate.opsForSet().size(key);
    }

    @SuppressWarnings("all")
    public Long sRem(String key,String... members){
        return redisTemplate.opsForSet().remove(key,members);
    }

    public Boolean sMove(String key1,String key2,String member){
        return redisTemplate.opsForSet().move(key1,member,key2);
    }

    public Set<Object> sDiff(String key1,String key2){
        return redisTemplate.opsForSet().difference(key1,key2);
    }

    public Set<Object> sDiff(Set<String> strings){
        return redisTemplate.opsForSet().difference(strings);
    }

    public Set<Object> sInter(String key1,String key2){
        return redisTemplate.opsForSet().difference(key1,key2);
    }

    public Set<Object> sInter(Set<String> strings){
        return redisTemplate.opsForSet().difference(strings);
    }

    public Set<Object> sUnion(String key1,String key2){
        return redisTemplate.opsForSet().difference(key1,key2);
    }

    public Set<Object> sUnion(Set<String> strings){
        return redisTemplate.opsForSet().difference(strings);
    }

    public void lPush(String key,String value){
        redisTemplate.opsForList().leftPush(key,value);
    }

    public void lPush(String key,String... values){
        redisTemplate.opsForList().leftPushAll(key,values);
    }

    public void rPush(String key,String value){
        redisTemplate.opsForList().rightPush(key,value);
    }

    public void rPush(String key,String...values){
        redisTemplate.opsForList().rightPushAll(key,values);
    }

    public List<Object> lRange(String key,long start,long end){
        return redisTemplate.opsForList().range(key,start,end);
    }

    public void rPushAll(String key,List<String> values){
        redisTemplate.opsForList().rightPushAll(key,values);
    }

    public void rPushAll(String key,String[] values){
        redisTemplate.opsForList().rightPushAll(key,values);
    }

    public String lPop(String key){
        return (String) redisTemplate.opsForList().leftPop(key);
    }

    public List<Object> lPop(String key,long count){
        return redisTemplate.opsForList().leftPop(key,count);
    }

    public String rPop(String key){
        return (String) redisTemplate.opsForList().rightPop(key);
    }

    public List<Object> rPop(String key,long count){
        return redisTemplate.opsForList().rightPop(key,count);
    }

    public Long lLen(String key){
        return redisTemplate.opsForList().size(key);
    }

    public String lIndex(String key,long index){
        return (String) redisTemplate.opsForList().index(key,index);
    }

    public Long indexOf(String key,String value){
        return redisTemplate.opsForList().indexOf(key,value);
    }

    public Long lRem(String key,long count,String value){
        return redisTemplate.opsForList().remove(key,count,value);
    }

    public Long lRem(String key,String value){
        return redisTemplate.opsForList().remove(key,0,value);
    }

    public void lTrim(String key,long start,long end){
        //保留范围内list
        redisTemplate.opsForList().trim(key,start,end);
    }

    public void rPopLPush(String key1,String key2){
        redisTemplate.opsForList().rightPopAndLeftPush(key1,key2);
    }

    public void lSet(String key,long index,String value){
        redisTemplate.opsForList().set(key,index,value);
    }

    public void lInsert(String key,String pivot,String value,boolean isBefore){
        if(isBefore) redisTemplate.opsForList().leftPush(key,pivot,value);
        else redisTemplate.opsForList().rightPush(key,pivot,value);
    }

    public void setBit(String key,long offset,boolean value){
        redisTemplate.opsForValue().setBit(key,offset,value);
    }

    public Boolean getBit(String key,long offset){
        return redisTemplate.opsForValue().getBit(key,offset);
    }

    public Long bitCount(String key){
        //统计为一的位数
        RedisConnection connection= this.getConnection();
        return connection.bitCount(key.getBytes());
    }

    public Long bitCount(String key,long start,long end){
        RedisConnection connection=this.getConnection();
        return connection.bitCount(key.getBytes(),start,end);
    }

    public void zAdd(String key,double score,String member){
        redisTemplate.opsForZSet().add(key,member,score);
    }

    public Set<Object> zRange(String key, long start, long end){
        return redisTemplate.opsForZSet().range(key,start,end);
    }

    public Set<Object> zRevRange(String key,long start,long end){
        return redisTemplate.opsForZSet().reverseRange(key,start,end);
    }

    public void zRem(String key,String... member){
        redisTemplate.opsForZSet().remove(key,member);
    }


    public Long zCard(String key){
        return redisTemplate.opsForZSet().zCard(key);
    }

    public Double zIncrBy(String key ,String field,double count){
        return   redisTemplate.opsForZSet().incrementScore(key,field,count);
    }

    public Long zCount(String key,double start,double end){
        return redisTemplate.opsForZSet().count(key,start,end);
    }

    public Long pfAdd(String key ,String...values){
        return redisTemplate.opsForHyperLogLog().add(key,values);
    }

    public Long pfCount(String key){
        return redisTemplate.opsForHyperLogLog().size(key);
    }

    public Long pfCount(String...keys){
        return redisTemplate.opsForHyperLogLog().size(keys);
    }

    public void pfMerge(String key,String...keys){
        redisTemplate.opsForHyperLogLog().union(key,keys);
    }

    public void geoAdd(String key, Point point, String member){
        redisTemplate.opsForGeo().add(key,point,member);
    }

    public void geoAdd(String key,Map<Object,Point> map){
        redisTemplate.opsForGeo().add(key,map);
    }

    public List<Point> geoPos(String key, String...members){
       return redisTemplate.opsForGeo().position(key,members);
    }

    public Distance geoDist(String key, String member1, String member2){
        return redisTemplate.opsForGeo().distance(key,member1,member2);
    }

    public Distance geoDist(String key, String member1, String member2, Metric metric){
        return redisTemplate.opsForGeo().distance(key,member1,member2,metric);
    }

    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoRadius(String key, Circle circle){
        return redisTemplate.opsForGeo().radius(key,circle);
    }

    public GeoResults<RedisGeoCommands.GeoLocation<Object>>
    geoResults(String key,String member,double radius){
        return redisTemplate.opsForGeo().radius(key,member,radius);
    }

    public GeoResults<RedisGeoCommands.GeoLocation<Object>>
    geoResults(String key,String member,Distance distance){
        return redisTemplate.opsForGeo().radius(key,member,distance);
    }

    public void delete(Set<String> keys){
        redisTemplate.delete(keys);
    }

    public void delete(String key){
        redisTemplate.delete(key);
    }

    public String serialization(Object object){
        try{
            if(object==null) return null;
            return objectMapper.writeValueAsString(object);
        }catch (Exception e){
            logger.error("e : ",e);
            return null;
        }
    }

    public <T> T deserialize(String string, JavaType type){
        try{
            if(string==null || string.trim().isEmpty()) return null;
            return this.objectMapper.readValue(string,type);
        }catch (Exception e){
            logger.error("序列化失败 : {}",e.getMessage(),e);
            return null;
        }
    }

    public RedisTemplate<String,Object> getRedisTemplate(){
        return this.redisTemplate;
    }

    public RedisConnection getConnection(){
        RedisConnectionFactory factory=redisTemplate.getConnectionFactory();
        return factory!=null ? factory.getConnection() :null;
    }
}
