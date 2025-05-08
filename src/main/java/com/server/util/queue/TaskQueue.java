package com.server.util.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskQueue<T> {

    private final ConcurrentLinkedQueue<T> queue=new ConcurrentLinkedQueue<>();
    private static final int OUT_QUEUE_MSX=200;


    public void setTask(T task){
        queue.offer(task);
    }

    public List<T> cleanQueue(){
        if(queue.isEmpty()) return null;
        List<T> list=new ArrayList<>();

        while (true){
            T task=queue.poll();
            if(task==null) break;
            list.add(task);
        }

        return !list.isEmpty() ? list : null;
    }
}
