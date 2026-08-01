package com.liuliupi.utils.storage;

import com.liuliupi.entity.Resource;
import com.qiniu.storage.model.FileInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QiniuUtilTest {

    @Test
    void buildNewResourcesStoresKeyWithoutDownloadPrefix() {
        FileInfo item = new FileInfo();
        item.key = "article/abc.jpg";
        item.fsize = 1024L;
        item.mimeType = "image/jpeg";

        List<Resource> result = QiniuUtil.buildNewResources(new FileInfo[]{item}, Collections.emptyList());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPath()).isEqualTo("article/abc.jpg");
        assertThat(result.get(0).getPath()).doesNotStartWith("http");
    }

    @Test
    void buildNewResourcesSkipsExistingAndZeroSize() {
        FileInfo existing = new FileInfo();
        existing.key = "article/old.jpg";
        existing.fsize = 10L;

        FileInfo zero = new FileInfo();
        zero.key = "article/zero.jpg";
        zero.fsize = 0L;

        List<Resource> result = QiniuUtil.buildNewResources(
                new FileInfo[]{existing, zero},
                Arrays.asList("article/old.jpg"));

        assertThat(result).isEmpty();
    }
}