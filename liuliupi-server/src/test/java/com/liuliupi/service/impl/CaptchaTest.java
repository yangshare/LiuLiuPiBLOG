package com.liuliupi.service.impl;

import com.liuliupi.config.PoetryResult;
import com.liuliupi.constants.CommonConst;
import com.liuliupi.utils.cache.PoetryCache;
import com.liuliupi.vo.CaptchaVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaTest {

    private static final String FIXED_TOKEN = "fixed-captcha-token";

    /**
     * captcha() 应生成 captchaToken、可直接用于 img.src 的 base64 图片，
     * 并把验证码文本写入缓存（key = CAPTCHA_KEY + captchaToken）。
     */
    @Test
    void captchaGeneratesTokenImageAndCachesCode() {
        UserServiceImpl service = new UserServiceImpl();

        PoetryResult<CaptchaVO> result = service.captcha();

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
        CaptchaVO vo = result.getData();
        assertThat(vo).isNotNull();
        assertThat(vo.getCaptchaToken()).isNotBlank();
        assertThat(vo.getImage()).startsWith("data:image/png;base64,");

        String cacheKey = CommonConst.CAPTCHA_KEY + vo.getCaptchaToken();
        String cachedCode = (String) PoetryCache.get(cacheKey);
        assertThat(cachedCode).isNotBlank();

        // 清理本用例写入的缓存
        PoetryCache.remove(cacheKey);
    }

    @AfterEach
    void cleanFixedToken() {
        PoetryCache.remove(CommonConst.CAPTCHA_KEY + FIXED_TOKEN);
    }

    /** captchaToken 不存在/过期 → "验证码已失效，请刷新！" */
    @Test
    void verifyReturnsExpiredWhenTokenNotCached() {
        String error = UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "ABCD");
        assertThat(error).isEqualTo("验证码已失效，请刷新！");
    }

    /** 验证码比对不一致 → "验证码错误！" */
    @Test
    void verifyReturnsErrorWhenCodeMismatch() {
        PoetryCache.put(CommonConst.CAPTCHA_KEY + FIXED_TOKEN, "ABCD", CommonConst.CAPTCHA_EXPIRE);
        String error = UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "WRONG");
        assertThat(error).isEqualTo("验证码错误！");
    }

    /** 大小写不一致但字母相同 → 通过（返回 null） */
    @Test
    void verifyPassesWhenCodeMatchesIgnoringCase() {
        PoetryCache.put(CommonConst.CAPTCHA_KEY + FIXED_TOKEN, "ABCD", CommonConst.CAPTCHA_EXPIRE);
        String error = UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "abcd");
        assertThat(error).isNull();
    }

    /** 一次性消费：无论对错，校验后立即删除，第二次用同一 token 必返回"已失效" */
    @Test
    void verifyConsumesTokenOnceSoSecondAttemptFails() {
        PoetryCache.put(CommonConst.CAPTCHA_KEY + FIXED_TOKEN, "ABCD", CommonConst.CAPTCHA_EXPIRE);
        // 第一次（错误码）消费掉 token
        UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "WRONG");
        // 第二次即使传正确码，token 已被删 → 已失效
        String error = UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "ABCD");
        assertThat(error).isEqualTo("验证码已失效，请刷新！");
    }

    /** 一次性消费必须是原子操作：并发使用同一 token 时，最多只有一个请求可通过 */
    @Test
    void verifyConsumesTokenAtomicallyWhenRequestsRace() throws InterruptedException {
        PoetryCache.put(CommonConst.CAPTCHA_KEY + FIXED_TOKEN, "ABCD", CommonConst.CAPTCHA_EXPIRE);

        int requestCount = 32;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<String> errors = Collections.synchronizedList(IntStream.range(0, requestCount)
                .mapToObj(i -> "")
                .collect(Collectors.toList()));

        for (int i = 0; i < requestCount; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    errors.set(index, UserServiceImpl.verifyCaptcha(FIXED_TOKEN, "ABCD"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.set(index, e.getMessage());
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(errors).filteredOn(error -> error == null).hasSize(1);
        assertThat(errors).filteredOn("验证码已失效，请刷新！"::equals).hasSize(requestCount - 1);
    }
}
