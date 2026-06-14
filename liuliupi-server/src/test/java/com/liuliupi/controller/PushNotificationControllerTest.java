package com.liuliupi.controller;

import com.liuliupi.config.PoetryResult;
import com.liuliupi.entity.PushNotification;
import com.liuliupi.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationControllerTest {

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private PushNotificationController controller;

    @Test
    void getPushNotificationShouldReturnEnabledRecord() {
        PushNotification push = new PushNotification();
        push.setId(1);
        push.setTitle("推送标题");
        push.setEnabled(true);

        when(pushNotificationService.getEnabled()).thenReturn(push);

        PoetryResult<PushNotification> result = controller.getPushNotification();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getTitle()).isEqualTo("推送标题");
    }

    @Test
    void getPushNotificationShouldReturnNullWhenDisabled() {
        when(pushNotificationService.getEnabled()).thenReturn(null);

        PoetryResult<PushNotification> result = controller.getPushNotification();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
    }

    @Test
    void adminGetPushNotificationShouldDelegateToService() {
        PushNotification push = new PushNotification();
        push.setId(1);

        when(pushNotificationService.getEnabled()).thenReturn(push);

        PoetryResult<PushNotification> result = controller.getAdminPushNotification();

        assertThat(result.getData()).isEqualTo(push);
    }

    @Test
    void savePushNotificationShouldDelegateToService() {
        PushNotification push = new PushNotification();
        push.setTitle("标题");

        PoetryResult<Void> result = controller.savePushNotification(push);

        assertThat(result.getCode()).isEqualTo(200);
        verify(pushNotificationService).saveOrUpdateSingle(push);
    }
}
