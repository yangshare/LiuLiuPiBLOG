package com.liuliupi.service.impl;

import com.liuliupi.config.PoetryResult;
import com.liuliupi.constants.CommonConst;
import com.liuliupi.utils.cache.PoetryCache;
import com.liuliupi.vo.CaptchaVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaTest {

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
}
