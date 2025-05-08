package com.server.interceptor;

import com.server.service.userservice.impl.UserServiceImpl;
import com.server.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    @Autowired
    private UserServiceImpl userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token=request.getHeader("X-Uskey");
        if(token!=null){
            String sub =JwtUtil.validateAndGetToken(token);
            if(sub!=null && userService.isAdmin(sub)){
                request.setAttribute("id",sub);
                return true;
            }
        }
        response.setStatus(403);
        response.getWriter().write("{\"code\": 404, \"message\": \"FORBIDDEN\"}");
        return false;
    }
}
