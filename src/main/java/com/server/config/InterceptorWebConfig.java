package com.server.config;

import com.server.interceptor.AdminAuthInterceptor;
import com.server.interceptor.AuthInterceptor;
import com.server.interceptor.LockInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorWebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Autowired
    private LockInterceptor lockInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .order(1)//级别
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/public/**");

//        registry.addInterceptor(lockInterceptor)
//                .order(2)//被冻结就无法发送弹幕和评论创作视频
//                .addPathPatterns("/api/");

        registry.addInterceptor(adminAuthInterceptor)
                .order(3)
                .addPathPatterns("/admin/**");
    }
}
