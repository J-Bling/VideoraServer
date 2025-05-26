package com.server.aspect;


import com.server.entity.user.UserRelation;
import com.server.message.entity.Message;
import com.server.message.service.ChatService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class CreateMessageByFollow {
    @Autowired private ChatService chatService;
    private final Logger logger = LoggerFactory.getLogger(CreateMessageByFollow.class);

    @AfterReturning(
            pointcut="execution(* com.server.service.interaction.impl.InteractionServiceImpl.handleUserRelation(com.server.entity.user.UserRelation))&& args(userRelation)",
            returning = "result"
    )
    public void sendMessageProxy(UserRelation userRelation,Boolean result){
        try{
            if(result ==null || !result) return;
            chatService.produceMessage(new Message(userRelation.getUser_id(),userRelation.getTarget_id(),"关注了你"));

        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }
}
