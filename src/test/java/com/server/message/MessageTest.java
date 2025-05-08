package com.server.message;

import com.server.message.entity.Message;
import com.server.message.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class MessageTest {

    @Autowired private ChatService chatService;

    @Test
    public void test() throws InterruptedException {
        new Thread(()->{
            long start =System.currentTimeMillis();
            List<Message> messages =chatService.findHistoryMessage(100020,100019,null,0);
            System.out.println(messages);

            System.out.println(messages.size());
            System.out.println(Thread.currentThread().getName()  + "  "+(System.currentTimeMillis()-start));
        }).start();


        new Thread(()->{
            long start =System.currentTimeMillis();

            System.out.println(chatService.findFriends(100020));
            System.out.println(Thread.currentThread().getName()  + "  "+(System.currentTimeMillis()-start));
        }).start();

        Thread.sleep(24*10000000);
    }

}
