package com.server.util.queue;
import com.server.dto.cache.StatsUpdateTask;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class UpdateQueue{

    private final ConcurrentLinkedQueue<StatsUpdateTask> queue =new ConcurrentLinkedQueue<>();
    public UpdateQueue(){}

    public void setUpdateTask(StatsUpdateTask statsUpdateTask){
        queue.offer(statsUpdateTask);
    }

    public List<StatsUpdateTask> cleanQueue(){
        List<StatsUpdateTask> list=new CopyOnWriteArrayList<>();
        while (true){
            StatsUpdateTask task=queue.poll();
            if(task==null) return list;
            list.add(task);
        }
    }
}
