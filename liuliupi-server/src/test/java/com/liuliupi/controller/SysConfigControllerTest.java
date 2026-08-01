package com.liuliupi.controller;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.entity.SysConfig;
import com.liuliupi.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysConfigControllerTest {

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private BaseMapper<SysConfig> baseMapper;

    @InjectMocks
    private SysConfigController controller;

    @Test
    void listSysConfigIncludesLocalDownloadUrlEvenWhenDbIsEmpty() {
        ReflectionTestUtils.setField(controller, "localDownloadUrl", "https://local.test/files");
        when(sysConfigService.getBaseMapper()).thenReturn(baseMapper);
        when(baseMapper.selectList(any())).thenReturn(Collections.emptyList());

        PoetryResult<Map<String, String>> result = controller.listSysConfig();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsEntry("local.downloadUrl", "https://local.test/files");
    }

    @Test
    void listSysConfigMergesPublicConfigsFromDbWithLocalDownloadUrl() {
        ReflectionTestUtils.setField(controller, "localDownloadUrl", "https://local.test/files");

        SysConfig qiniu = new SysConfig();
        qiniu.setConfigKey("qiniu.downloadUrl");
        qiniu.setConfigValue("https://cdn.example.com/");

        when(sysConfigService.getBaseMapper()).thenReturn(baseMapper);
        when(baseMapper.selectList(any())).thenReturn(Collections.singletonList(qiniu));

        PoetryResult<Map<String, String>> result = controller.listSysConfig();

        assertThat(result.getData())
                .containsEntry("qiniu.downloadUrl", "https://cdn.example.com/")
                .containsEntry("local.downloadUrl", "https://local.test/files");
    }
}
