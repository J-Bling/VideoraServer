package com.server.controller.api.message;

import com.server.dto.response.user.UserResponse;
import com.server.entity.constant.WebConstant;
import com.server.message.entity.Message;
import com.server.message.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "chat")
public class HistoryMessageController {
    @Autowired private ChatService chatService;

    private final Logger logger= LoggerFactory.getLogger(HistoryMessageController.class);

    @GetMapping("/friends")
    public List<UserResponse> getFriendList(
            HttpServletRequest request
    ){
        try{
            int userId= Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            return chatService.findFriends(userId);
        }catch (Exception e){
            logger.error("find friends fail reason is :{}",e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/messages/{targetId}/{lastCreated}/{offset}")
    public List<Message> getHistoryMessages(
            HttpServletRequest request,
            @PathVariable("targetId") int targetId,
            @PathVariable("lastCreated") Long lastCreated,
            @PathVariable("offset") int offset
    ){
        try{
            int userId= Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            return chatService.findHistoryMessage(userId,targetId,lastCreated,offset);
        }catch (Exception e){
            logger.error("getHistoryMessages fail reason is :{}",e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"获取历史数据失败");
        }
    }
}
