package com.liuliupi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liuliupi.dao.PushNotificationMapper;
import com.liuliupi.entity.PushNotification;
import com.liuliupi.service.PushNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 首页弹窗推送配置表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2026-06-14
 */
@Service
public class PushNotificationServiceImpl extends ServiceImpl<PushNotificationMapper, PushNotification> implements PushNotificationService {

    @Override
    public void saveOrUpdateSingle(PushNotification pushNotification) {
        List<PushNotification> list = baseMapper.selectList(new QueryWrapper<PushNotification>()
                .orderByAsc("id")
                .last("LIMIT 1"));
        if (CollectionUtils.isEmpty(list)) {
            baseMapper.insert(pushNotification);
        } else {
            pushNotification.setId(list.get(0).getId());
            baseMapper.updateById(pushNotification);
        }
    }

    @Override
    public PushNotification getEnabled() {
        List<PushNotification> list = baseMapper.selectList(new QueryWrapper<PushNotification>()
                .eq("enabled", 1)
                .orderByAsc("id")
                .last("LIMIT 1"));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }
}
