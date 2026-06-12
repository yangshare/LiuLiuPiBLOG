package com.liuliupi.aop;

import com.liuliupi.constants.CommonConst;
import com.liuliupi.entity.User;
import com.liuliupi.enums.CodeMsg;
import com.liuliupi.handle.PoetryLoginException;
import com.liuliupi.handle.PoetryRuntimeException;
import com.liuliupi.utils.PoetryUtil;
import com.liuliupi.utils.cache.PoetryCache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Aspect
@Component
@Order(0)
@Slf4j
public class LoginCheckAspect {

    @Around("@annotation(loginCheck)")
    public Object around(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
        String token = PoetryUtil.getToken();
        if (!StringUtils.hasText(token)) {
            throw new PoetryLoginException(CodeMsg.NOT_LOGIN.getMsg());
        }

        if (!token.startsWith(CommonConst.ACCESS_TOKEN)) {
            throw new PoetryLoginException(CodeMsg.NOT_LOGIN.getMsg());
        }

        User user = (User) PoetryCache.get(token);

        if (user == null) {
            throw new PoetryLoginException(CodeMsg.LOGIN_EXPIRED.getMsg());
        }

        // 统一权限判断：直接使用 userType
        // loginCheck.value() 表示所需的最低权限级别（0=站长, 1=管理员, 2=普通用户）
        // userType 值越小权限越高：0 < 1 < 2
        if (loginCheck.value() < user.getUserType()) {
            throw new PoetryRuntimeException("权限不足！");
        }

        // 重置过期时间（统一使用一套 key）
        String userId = user.getId().toString();
        boolean needRefresh = PoetryCache.get(CommonConst.TOKEN_INTERVAL_KEY + userId) == null;

        if (needRefresh) {
            synchronized (userId.intern()) {
                // 双重检查
                if (PoetryCache.get(CommonConst.TOKEN_INTERVAL_KEY + userId) == null) {
                    PoetryCache.put(token, user, CommonConst.TOKEN_EXPIRE);
                    PoetryCache.put(CommonConst.TOKEN + userId, token, CommonConst.TOKEN_EXPIRE);
                    PoetryCache.put(CommonConst.TOKEN_INTERVAL_KEY + userId, token, CommonConst.TOKEN_INTERVAL);
                }
            }
        }

        return joinPoint.proceed();
    }
}
