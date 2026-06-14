package com.liuliupi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liuliupi.entity.PushNotification;

/**
 * <p>
 * 首页弹窗推送配置表 服务类
 * </p>
 *
 * @author sara
 * @since 2026-06-14
 */
public interface PushNotificationService extends IService<PushNotification> {

    /**
     * 保存或更新单条推送配置（表中始终只有一条记录）
     */
    void saveOrUpdateSingle(PushNotification pushNotification);

    /**
     * 获取当前启用的推送配置
     */
    PushNotification getEnabled();
}
