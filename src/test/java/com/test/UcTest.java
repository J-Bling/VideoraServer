package com.test;

import com.server.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class thread implements Runnable{
    private List<String> strings;

    thread(List<String> strings){this.strings=strings;}

    @Override
    public void run(){
        strings.add(Runnable.class.toString());
    }
}

public class UcTest {
    @Test
    public void test1(){
        List<String> strings=new ArrayList<>();
        new Thread(new thread(strings)).start();
        new Thread(new thread(strings)).start();
        for(String string : strings){
            System.out.println(string);
        }
        System.out.println(strings.size());
    }

    @Test
    public void test2(){
        System.out.println(NotificationType.SYSTEM.toString().equals(NotificationType.SYSTEM.getValue()));
    }
}
