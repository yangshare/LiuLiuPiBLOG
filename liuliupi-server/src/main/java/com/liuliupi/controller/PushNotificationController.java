package com.liuliupi.controller;

import com.liuliupi.aop.LoginCheck;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.entity.PushNotification;
import com.liuliupi.service.PushNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 首页弹窗推送配置 前端控制器
 * </p>
 *
 * @author sara
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/pushNotification")
public class PushNotificationController {

    @Autowired
    private PushNotificationService pushNotificationService;

    /**
     * 前台获取当前启用的推送
     */
    @GetMapping("/getPushNotification")
    public PoetryResult<PushNotification> getPushNotification() {
        return PoetryResult.success(pushNotificationService.getEnabled());
    }

    /**
     * 后台获取当前推送配置
     */
    @GetMapping("/admin/getPushNotification")
    @LoginCheck(0)
    public PoetryResult<PushNotification> getAdminPushNotification() {
        return PoetryResult.success(pushNotificationService.getEnabled());
    }

    /**
     * 后台保存/更新推送配置
     */
    @PostMapping("/admin/savePushNotification")
    @LoginCheck(0)
    public PoetryResult<Void> savePushNotification(@RequestBody PushNotification pushNotification) {
        if (!StringUtils.hasText(pushNotification.getTitle())) {
            return PoetryResult.fail("推送标题不能为空");
        }
        pushNotificationService.saveOrUpdateSingle(pushNotification);
        return PoetryResult.success();
    }
}
