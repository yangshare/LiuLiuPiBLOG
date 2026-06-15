package com.liuliupi.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.liuliupi.dao.PushNotificationMapper;
import com.liuliupi.entity.PushNotification;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
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

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), PushNotification.class);
    }

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
    void saveOrUpdateShouldDefaultNullEnabledToFalseBeforeInsert() {
        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.emptyList());

        PushNotification push = new PushNotification();
        push.setTitle("");
        push.setCover("");
        push.setUrl("");

        pushNotificationService.saveOrUpdateSingle(push);

        assertThat(push.getEnabled()).isFalse();
        verify(pushNotificationMapper).insert(push);
    }

    @Test
    void saveOrUpdateShouldRetryAsUpdateWhenConcurrentInsertCreatesRecord() {
        PushNotification existing = new PushNotification();
        existing.setId(1);

        when(pushNotificationMapper.selectList(any()))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList(existing));

        PushNotification push = new PushNotification();
        push.setTitle("并发推送");
        push.setCover("https://example.com/cover.jpg");
        push.setUrl("https://example.com/");
        push.setEnabled(true);

        doThrow(new DuplicateKeyException("uk_push_notification_singleton"))
                .when(pushNotificationMapper).insert(push);

        pushNotificationService.saveOrUpdateSingle(push);

        assertThat(push.getId()).isEqualTo(1);
        verify(pushNotificationMapper).updateById(push);
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

        assertThat(push.getId()).isEqualTo(1);
        verify(pushNotificationMapper, never()).insert(any());
        verify(pushNotificationMapper).updateById(push);
    }

    @Test
    void saveOrUpdateShouldRemoveExtraRecords() {
        PushNotification first = new PushNotification();
        first.setId(1);
        PushNotification extra = new PushNotification();
        extra.setId(2);

        when(pushNotificationMapper.selectList(any())).thenReturn(Arrays.asList(first, extra));

        PushNotification push = new PushNotification();
        push.setTitle("新标题");
        push.setEnabled(true);

        pushNotificationService.saveOrUpdateSingle(push);

        assertThat(push.getId()).isEqualTo(1);
        verify(pushNotificationMapper).updateById(push);
        verify(pushNotificationMapper).deleteBatchIds(Collections.singletonList(2));
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
        assertThat(result.getTitle()).isEqualTo("推送");
        assertThat(result.getEnabled()).isTrue();
    }

    @Test
    void getEnabledShouldReturnNullWhenDisabled() {
        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.emptyList());

        PushNotification result = pushNotificationService.getEnabled();

        assertThat(result).isNull();
    }

    @Test
    void getSingleShouldReturnFirstRecordEvenWhenDisabled() {
        PushNotification push = new PushNotification();
        push.setId(1);
        push.setTitle("推送");
        push.setEnabled(false);

        when(pushNotificationMapper.selectList(any())).thenReturn(Collections.singletonList(push));

        PushNotification result = pushNotificationService.getSingle();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getEnabled()).isFalse();
    }
}
