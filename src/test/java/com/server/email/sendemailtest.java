package com.server.email;

import com.server.util.EmailUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;

@SpringBootTest
public class sendemailtest {
    @Autowired
    private EmailUtil emailUtil;

    private final Logger logger= LoggerFactory.getLogger(sendemailtest.class);

    @Test
    public void send(){
        try{
            CountDownLatch latch=new CountDownLatch(1);
            String code=emailUtil.generateCode();
            String testEmail="3383492574@qq.com";
            emailUtil.send(testEmail,latch);
            latch.await();
            logger.info("执行完毕");
        }catch (Exception e){
            logger.error("邮件发送失败,失败原因 : {}  ,error :",e.getMessage(),e);
        }
    }
}
