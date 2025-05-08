package com.server.dto.cache;


public class Cache<T>{
    protected Long cacheTime;
    protected T cache;

    public Cache(T cache){
        this.cache=cache;
        this.cacheTime=System.currentTimeMillis()/1000;
    }

    public Long getCacheTime() {
        return cacheTime;
    }

    public T getCache() {
        return cache;
    }

    public void setCache(T cache) {
        this.cache = cache;
    }

    public void setCacheTime(Long cacheTime) {
        this.cacheTime = cacheTime;
    }
}
