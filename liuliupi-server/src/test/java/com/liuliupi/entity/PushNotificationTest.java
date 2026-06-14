package com.liuliupi.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 10, 30, 0);
        push.setCreateTime(now);
        push.setUpdateTime(now);

        assertThat(push.getId()).isEqualTo(1);
        assertThat(push.getTitle()).isEqualTo("测试标题");
        assertThat(push.getCover()).isEqualTo("https://example.com/cover.jpg");
        assertThat(push.getUrl()).isEqualTo("https://example.com/");
        assertThat(push.getEnabled()).isTrue();
        assertThat(push.getCreateTime()).isEqualTo(now);
        assertThat(push.getUpdateTime()).isEqualTo(now);
    }

    @Test
    void equalsAndHashCodeShouldWork() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 10, 30, 0);

        PushNotification a = new PushNotification();
        a.setId(1);
        a.setTitle("测试标题");
        a.setCover("https://example.com/cover.jpg");
        a.setUrl("https://example.com/");
        a.setEnabled(true);
        a.setCreateTime(now);
        a.setUpdateTime(now);

        PushNotification b = new PushNotification();
        b.setId(1);
        b.setTitle("测试标题");
        b.setCover("https://example.com/cover.jpg");
        b.setUrl("https://example.com/");
        b.setEnabled(true);
        b.setCreateTime(now);
        b.setUpdateTime(now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        b.setId(2);
        assertThat(a).isNotEqualTo(b);
    }
}
