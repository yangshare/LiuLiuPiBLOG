package com.liuliupi.utils.storage;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.liuliupi.entity.Resource;
import com.liuliupi.service.ResourceService;
import com.liuliupi.vo.FileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalUtilTest {

    @Mock
    private ResourceService resourceService;

    @TempDir
    Path tempDir;

    private LocalUtil newLocalUtil(String downloadUrl) {
        LocalUtil localUtil = new LocalUtil();
        String uploadUrl = tempDir.toString().replace('\\', '/') + "/";
        ReflectionTestUtils.setField(localUtil, "uploadUrl", uploadUrl);
        ReflectionTestUtils.setField(localUtil, "downloadUrl", downloadUrl);
        ReflectionTestUtils.setField(localUtil, "resourceService", resourceService);
        return localUtil;
    }

    @Test
    void saveFileReturnsVisitPathWithoutDownloadPrefix() {
        LocalUtil localUtil = newLocalUtil("https://cdn.example.com/");

        FileVO fileVO = new FileVO();
        fileVO.setRelativePath("article/test.jpg");
        fileVO.setFile(new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}));

        FileVO result = localUtil.saveFile(fileVO);

        // visitPath 必须是 key（不含域名），写库据此渲染
        assertThat(result.getVisitPath()).isEqualTo("article/test.jpg");
        assertThat(result.getAbsolutePath()).endsWith("article/test.jpg");
        assertThat(result.getVisitPath()).doesNotStartWith("http");
    }

    @Test
    void deleteFileStripsDownloadPrefixThenDeletesFileAndResourceRecord() {
        LocalUtil localUtil = newLocalUtil("https://cdn.example.com/");
        String uploadUrl = tempDir.toString().replace('\\', '/') + "/";

        // 模拟对象存储中已存在的文件
        new File(uploadUrl + "comment").mkdirs();
        try {
            new File(uploadUrl + "comment/pic.jpg").createNewFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LambdaUpdateChainWrapper<Resource> chain = newLambdaChainMock();

        // 传入带旧域名的完整 URL，验证 stripDownloadPrefix 生效后仍能定位文件
        localUtil.deleteFile(Collections.singletonList("https://cdn.example.com/comment/pic.jpg"));

        assertThat(new File(uploadUrl + "comment/pic.jpg")).doesNotExist();
        // 按 key（剥前缀后）删除 resource 记录
        verify(resourceService).lambdaUpdate();
        verify(chain).eq(any(), eq("comment/pic.jpg"));
        verify(chain).remove();
    }

    @Test
    void deleteFileAcceptsBareKeyWithoutPrefix() {
        LocalUtil localUtil = newLocalUtil("https://cdn.example.com/");
        String uploadUrl = tempDir.toString().replace('\\', '/') + "/";

        new File(uploadUrl + "article").mkdirs();
        try {
            new File(uploadUrl + "article/bare.jpg").createNewFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LambdaUpdateChainWrapper<Resource> chain = newLambdaChainMock();

        localUtil.deleteFile(Collections.singletonList("article/bare.jpg"));

        assertThat(new File(uploadUrl + "article/bare.jpg")).doesNotExist();
        verify(chain).eq(any(), eq("article/bare.jpg"));
        verify(chain).remove();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaUpdateChainWrapper<Resource> newLambdaChainMock() {
        LambdaUpdateChainWrapper<Resource> chain = org.mockito.Mockito.mock(LambdaUpdateChainWrapper.class);
        when(resourceService.lambdaUpdate()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        return chain;
    }
}