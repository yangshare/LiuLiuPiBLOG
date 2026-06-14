package com.liuliupi.service.impl;

import com.liuliupi.dao.PushNotificationMapper;
import com.liuliupi.entity.PushNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceImplTest {

    @Mock
    private PushNotificationMapper pushNotificationMapper;

    @InjectMocks
    private PushNotificationServiceImpl pushNotificationService;

    @Test
    void saveOrUpdateShouldInsertWhenNoRecordExists() {
        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.emptyList());

        PushNotification push = new PushNotification();
        push.setTitle("标题");
        push.setCover("https://example.com/cover.jpg");
        push.setUrl("https://example.com/");
        push.setEnabled(true);

        pushNotificationService.saveOrUpdateSingle(push);

        verify(pushNotificationMapper).insert(push);
        verify(pushNotificationMapper, never()).updateById(any());
    }

    @Test
    void saveOrUpdateShouldUpdateWhenRecordExists() {
        PushNotification existing = new PushNotification();
        existing.setId(1);

        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        PushNotification push = new PushNotification();
        push.setTitle("新标题");
        push.setEnabled(false);

        pushNotificationService.saveOrUpdateSingle(push);

        verify(pushNotificationMapper, never()).insert(any());
        verify(pushNotificationMapper).updateById(push);
    }

    @Test
    void getEnabledShouldReturnFirstEnabledRecord() {
        PushNotification push = new PushNotification();
        push.setId(1);
        push.setTitle("推送");
        push.setEnabled(true);

        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.singletonList(push));

        PushNotification result = pushNotificationService.getEnabled();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    void getEnabledShouldReturnNullWhenDisabled() {
        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.emptyList());

        PushNotification result = pushNotificationService.getEnabled();

        assertThat(result).isNull();
    }
}
