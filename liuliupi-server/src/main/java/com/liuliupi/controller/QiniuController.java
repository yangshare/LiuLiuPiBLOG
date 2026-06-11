package com.liuliupi.controller;

import com.liuliupi.aop.LoginCheck;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.aop.SaveCheck;
import com.liuliupi.utils.storage.QiniuUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 七牛云
 */
@RestController
@RequestMapping("/qiniu")
@ConditionalOnBean(QiniuUtil.class)
public class QiniuController {

    @Autowired
    private QiniuUtil qiniuUtil;

    /**
     * 获取覆盖凭证，用于七牛云
     */
    @GetMapping("/getUpToken")
    @LoginCheck
    @SaveCheck
    public PoetryResult<String> getUpToken(@RequestParam(value = "key") String key) {
        return PoetryResult.success(qiniuUtil.getToken(key));
    }
}
