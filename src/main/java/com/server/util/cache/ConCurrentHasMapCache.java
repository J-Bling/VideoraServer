package com.server.util.cache;

import com.server.dto.cache.Cache;
import java.util.concurrent.ConcurrentHashMap;

public class ConCurrentHasMapCache<T>{

    protected final ConcurrentHashMap<Integer, Cache<T>> Cache=new ConcurrentHashMap<>();
    protected long RETAIN_TIME=1800;//保留半小时

    public T findCache(Integer id){
        Cache<T> cache=this.Cache.get(id);
        return cache != null ? cache.getCache() : null;
    }

    public void setCache(Integer id,T cache){
        Cache.putIfAbsent(id,new Cache<T>(cache));
    }

    public void cleanCache(){
        if(!Cache.isEmpty()) {
            long time = System.currentTimeMillis() / 1000;
            Cache.forEach(1, (key, value) -> {
                if (value != null) {
                    if (time - value.getCacheTime() >= RETAIN_TIME) {
                        Cache.remove(key);
                    }
                } else {
                    Cache.remove(key);
                }
            });
        }
    }
}
