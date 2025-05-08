package com.server.controller.api.message;


import com.server.dto.response.Result;
import com.server.entity.constant.WebConstant;
import com.server.enums.ErrorCode;
import com.server.exception.ApiException;
import com.server.message.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/history-message")
@Tag(name = "历史私信记录")
public class HistoryMessageController {
    @Autowired private ChatService chatService;

    private final Logger logger= LoggerFactory.getLogger(HistoryMessageController.class);

    @GetMapping("/friend-list")
    public ResponseEntity<Result> getFriendList(
            HttpServletRequest request
    ){
        try{
            int userId= Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            return Result.Ok(chatService.findFriends(userId));
        }catch (Exception e){
            logger.error("find friends fail reason is :{}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }

    @GetMapping("/messages/{targetId}/{lastCreated}/{offset}")
    public ResponseEntity<Result> getHistoryMessages(
            HttpServletRequest request,
            @PathVariable("targetId") int targetId,
            @PathVariable("lastCreated") Long lastCreated,
            @PathVariable("offset") int offset
    ){
        try{
            int userId= Integer.parseInt(request.getAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID).toString());
            return Result.Ok(chatService.findHistoryMessage(userId,targetId,lastCreated,offset));
        }catch (ApiException apiException){
            return Result.ErrorResult(apiException.getErrorCode(),0);
        }catch (Exception e){
            logger.error("getHistoryMessages fail reason is :{}",e.getMessage());
            return Result.ErrorResult(ErrorCode.INTERNAL_SERVER_ERROR,0);
        }
    }
}
