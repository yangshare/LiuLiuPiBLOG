package com.liuliupi.aop;

import com.liuliupi.constants.CommonConst;
import com.liuliupi.entity.User;
import com.liuliupi.enums.PoetryEnum;
import com.liuliupi.handle.PoetryLoginException;
import com.liuliupi.utils.cache.PoetryCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginCheckAspectTest {

    private final LoginCheckAspect loginCheckAspect = new LoginCheckAspect();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        PoetryCache.remove(CommonConst.ADMIN);
        PoetryCache.remove(CommonConst.TOKEN + CommonConst.ADMIN_USER_ID);
        PoetryCache.remove(CommonConst.TOKEN_INTERVAL_KEY + CommonConst.ADMIN_USER_ID);
    }

    @Test
    void rejectsNonTokenCacheKeysEvenWhenTheyContainAUser() {
        User admin = new User();
        admin.setId(CommonConst.ADMIN_USER_ID);
        admin.setUserType(PoetryEnum.USER_TYPE_ADMIN.getCode());
        PoetryCache.put(CommonConst.ADMIN, admin);

        setAuthorization(CommonConst.ADMIN);

        assertThatThrownBy(() -> loginCheckAspect.around(joinPoint(), loginCheck(0)))
                .isInstanceOf(PoetryLoginException.class);
    }

    private void setAuthorization(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CommonConst.TOKEN_HEADER, token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private ProceedingJoinPoint joinPoint() {
        return mock(ProceedingJoinPoint.class);
    }

    private LoginCheck loginCheck(int value) {
        LoginCheck loginCheck = mock(LoginCheck.class);
        when(loginCheck.value()).thenReturn(value);
        return loginCheck;
    }
}
