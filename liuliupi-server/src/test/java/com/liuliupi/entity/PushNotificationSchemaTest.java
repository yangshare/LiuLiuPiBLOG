package com.liuliupi.entity;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

class PushNotificationSchemaTest {

    @Test
    void schemaShouldEnforceSinglePushNotificationRecord() {
        InputStream inputStream = getClass().getResourceAsStream("/sql/liuliupi_blog.sql");

        assertThat(inputStream).isNotNull();
        try (Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A")) {
            String sql = scanner.hasNext() ? scanner.next() : "";

            assertThat(sql).contains("`singleton_key` tinyint NOT NULL DEFAULT 1");
            assertThat(sql).contains("`enabled` tinyint(1) NOT NULL DEFAULT 0");
            assertThat(sql).contains("UNIQUE KEY `uk_push_notification_singleton` (`singleton_key`)");
        }
    }
}
