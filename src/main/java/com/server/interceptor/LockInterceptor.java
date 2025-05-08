package com.server.interceptor;

import com.server.service.userservice.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LockInterceptor implements HandlerInterceptor {
    @Autowired
    private UserServiceImpl userService;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Integer id=(Integer) request.getAttribute("id");
        if(id!=null){
            boolean isLock=this.userService.isLock(id);
            request.setAttribute("isLock",isLock);
            return true;
        }
        response.setStatus(401);
        response.getWriter().write("{\"code\": 401, \"message\": \"Token is missing\"}");
        return false;
    }
}
