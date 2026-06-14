package com.liuliupi.vo;

import lombok.Data;

/**
 * 图形验证码响应
 */
@Data
public class CaptchaVO {

    /**
     * 验证码 token，登录时回传用于校验
     */
    private String captchaToken;

    /**
     * base64 data-url（data:image/png;base64,...），前端 img.src 可直接使用
     */
    private String image;
}
