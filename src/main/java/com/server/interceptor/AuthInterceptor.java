package com.server.interceptor;

import com.server.entity.constant.WebConstant;
import com.server.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/*
org.springframework.web.servlet.HandlerInterceptor      // 拦截器接口
org.springframework.web.servlet.handler.HandlerInterceptorAdapter  // 适配器类（已废弃，Spring 5.3+）
org.springframework.web.servlet.config.annotation.WebMvcConfigurer // 配置接口
org.springframework.web.servlet.config.annotation.InterceptorRegistry // 注册器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String token = request.getHeader(WebConstant.REQUEST_HEAD_AUTH);//自定义请求头,吧jwt放在里面

        if(token!=null){
            String sub=JwtUtil.validateAndGetToken(token);
            if(sub!=null){
                request.setAttribute(WebConstant.REQUEST_ATTRIBUTE_AUTH_ID,sub);
                return true;
            }
        }
        response.setStatus(401);
        response.getWriter().write("{\"code\": 401, \"message\": \"Token is missing\"}");
        return false;
    }
}
//
//    public void postHandle(HttpServletRequest request,
//                           HttpServletResponse response,
//                           Object handler,
//                           @Nullable ModelAndView modelAndView) throws Exception {
//        /*
//        controller正常返回后调用
//         */
//    }
//
//    public void afterCompletion(HttpServletRequest request,
//                                HttpServletResponse response,
//                                Object handler,
//                                @Nullable Exception ex) throws Exception {
//        /*
//        无论controller是否抛出错误都调用
//         */
//    }
