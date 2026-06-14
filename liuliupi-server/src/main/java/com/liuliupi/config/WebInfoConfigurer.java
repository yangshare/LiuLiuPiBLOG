package com.liuliupi.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class WebInfoConfigurer implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new WebInfoHandlerInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/user/captcha", "/user/login", "/admin/**", "/webInfo/getWebInfo", "/webInfo/updateWebInfo");
    }
}
