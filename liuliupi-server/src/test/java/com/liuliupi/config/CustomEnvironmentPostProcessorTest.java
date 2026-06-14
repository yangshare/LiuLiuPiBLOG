package com.liuliupi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class CustomEnvironmentPostProcessorTest {

    @Test
    void sqlInitializationScriptIsAvailableOnTheClasspath() {
        assertThat(new ClassPathResource("sql/liuliupi_blog.sql").exists()).isTrue();
    }

    @Test
    void databaseProbeMatchesConfiguredDatasourceSchema() {
        Properties properties = new YamlPropertiesFactoryBean() {{
            setResources(new ClassPathResource("application.yml"));
        }}.getObject();

        assertThat(properties).isNotNull();

        String datasourceUrl = properties.getProperty("spring.datasource.url");
        String configuredDatabase = (String) ReflectionTestUtils.getField(CustomEnvironmentPostProcessor.class, "DATABASE");

        assertThat(configuredDatabase).isEqualTo(extractSchema(datasourceUrl));
    }

    private String extractSchema(String datasourceUrl) {
        int queryIndex = datasourceUrl.indexOf('?');
        String baseUrl = queryIndex >= 0 ? datasourceUrl.substring(0, queryIndex) : datasourceUrl;
        int slashIndex = baseUrl.lastIndexOf('/');
        return baseUrl.substring(slashIndex + 1);
    }
}
