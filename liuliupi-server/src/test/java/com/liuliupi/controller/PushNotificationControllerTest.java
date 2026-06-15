package com.liuliupi.controller;

import com.liuliupi.config.PoetryResult;
import com.liuliupi.entity.PushNotification;
import com.liuliupi.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PushNotificationControllerTest {

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
        push.setEnabled(false);

        when(pushNotificationService.getSingle()).thenReturn(push);

        PoetryResult<PushNotification> result = controller.getAdminPushNotification();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(push);
        verify(pushNotificationService).getSingle();
        verify(pushNotificationService, never()).getEnabled();
    }

    @Test
    void savePushNotificationShouldAllowEmptyContentWhenDisabled() {
        PushNotification push = new PushNotification();
        push.setTitle("");
        push.setCover("");
        push.setUrl("");
        push.setEnabled(false);

        PoetryResult<Void> result = controller.savePushNotification(push);

        assertThat(result.getCode()).isEqualTo(200);
        verify(pushNotificationService).saveOrUpdateSingle(push);
    }

    @Test
    void savePushNotificationShouldDefaultNullEnabledToDisabled() {
        PushNotification push = new PushNotification();
        push.setTitle(null);

        PoetryResult<Void> result = controller.savePushNotification(push);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(push.getEnabled()).isFalse();
        verify(pushNotificationService).saveOrUpdateSingle(push);
    }

    @Test
    void savePushNotificationShouldFailWhenEnabledContentIsIncomplete() {
        PushNotification push = new PushNotification();
        push.setTitle("推送");
        push.setCover("");
        push.setUrl("https://example.com/");
        push.setEnabled(true);

        PoetryResult<Void> result = controller.savePushNotification(push);

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("启用推送时请完善标题、封面和跳转链接");
        verify(pushNotificationService, never()).saveOrUpdateSingle(any());
    }

    @Test
    void savePushNotificationShouldDelegateToService() {
        PushNotification push = new PushNotification();
        push.setTitle("标题");
        push.setCover("https://example.com/cover.jpg");
        push.setUrl("https://example.com/");
        push.setEnabled(true);

        PoetryResult<Void> result = controller.savePushNotification(push);

        assertThat(result.getCode()).isEqualTo(200);
        verify(pushNotificationService).saveOrUpdateSingle(push);
    }

    @Test
    void routesShouldMatchDesignPaths() throws NoSuchMethodException {
        Method getPublic = PushNotificationController.class.getMethod("getPushNotification");
        Method getAdmin = PushNotificationController.class.getMethod("getAdminPushNotification");
        Method saveAdmin = PushNotificationController.class.getMethod("savePushNotification", PushNotification.class);

        assertThat(getPublic.getAnnotation(GetMapping.class).value()).containsExactly("/pushNotification/getPushNotification");
        assertThat(getAdmin.getAnnotation(GetMapping.class).value()).containsExactly("/admin/pushNotification/getPushNotification");
        assertThat(saveAdmin.getAnnotation(PostMapping.class).value()).containsExactly("/admin/pushNotification/savePushNotification");
    }
}
