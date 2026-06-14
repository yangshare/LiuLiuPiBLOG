package com.liuliupi.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushNotificationTest {

    @Test
    void entityShouldHoldFields() {
        PushNotification push = new PushNotification();
        push.setId(1);
        push.setTitle("测试标题");
        push.setCover("https://example.com/cover.jpg");
        push.setUrl("https://example.com/");
        push.setEnabled(true);

        assertThat(push.getId()).isEqualTo(1);
        assertThat(push.getTitle()).isEqualTo("测试标题");
        assertThat(push.getCover()).isEqualTo("https://example.com/cover.jpg");
        assertThat(push.getUrl()).isEqualTo("https://example.com/");
        assertThat(push.getEnabled()).isTrue();
    }
}
