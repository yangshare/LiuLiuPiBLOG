package com.liuliupi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liuliupi.dao.PushNotificationMapper;
import com.liuliupi.entity.PushNotification;
import com.liuliupi.service.PushNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * 保存或更新单条推送配置。
     * <p>
     * 该接口用于后台管理，并发调用概率低；单条记录保证依赖业务层串行处理。
     * 单 JVM 内通过 synchronized 串行化，配合 @Transactional 保证数据一致性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized void saveOrUpdateSingle(PushNotification pushNotification) {
        List<PushNotification> list = baseMapper.selectList(new LambdaQueryWrapper<PushNotification>()
                .orderByAsc(PushNotification::getId)
                .last("limit 1"));
        if (CollectionUtils.isEmpty(list)) {
            baseMapper.insert(pushNotification);
        } else {
            pushNotification.setId(list.get(0).getId());
            baseMapper.updateById(pushNotification);
        }
    }

    @Override
    public PushNotification getEnabled() {
        List<PushNotification> list = baseMapper.selectList(new LambdaQueryWrapper<PushNotification>()
                .eq(PushNotification::getEnabled, true)
                .orderByAsc(PushNotification::getId)
                .last("limit 1"));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }
}
