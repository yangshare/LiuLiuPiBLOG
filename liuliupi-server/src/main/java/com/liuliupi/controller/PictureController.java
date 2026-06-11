package com.liuliupi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.liuliupi.aop.LoginCheck;
import com.liuliupi.aop.SaveCheck;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.constants.CommonConst;
import com.liuliupi.dao.ResourcePathMapper;
import com.liuliupi.entity.ResourcePath;
import com.liuliupi.utils.PoetryUtil;
import com.liuliupi.vo.ResourcePathVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 资源聚合里的图片，其他接口在ResourceAggregationController
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class PictureController {

    @Autowired
    private ResourcePathMapper resourcePathMapper;

    /**
     * 查询爱情
     */
    @GetMapping("/listAdminLovePhoto")
    public PoetryResult<List<Map<String, Object>>> listAdminLovePhoto() {
        QueryWrapper<ResourcePath> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("classify, count(*) as count")
                .eq("status", Boolean.TRUE)
                .eq("remark", PoetryUtil.getAdminUser().getId().toString())
                .eq("type", CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO)
                .groupBy("classify");
        List<Map<String, Object>> maps = resourcePathMapper.selectMaps(queryWrapper);

        return PoetryResult.success(maps);
    }

    /**
     * 保存爱情
     */
    @LoginCheck
    @SaveCheck
    @PostMapping("/saveLovePhoto")
    public PoetryResult saveLovePhoto(@RequestBody ResourcePathVO resourcePathVO) {
        if (!StringUtils.hasText(resourcePathVO.getClassify()) || !StringUtils.hasText(resourcePathVO.getCover()) ||
                !StringUtils.hasText(resourcePathVO.getTitle())) {
            return PoetryResult.fail("信息不全！");
        }
        ResourcePath lovePhoto = new ResourcePath();
        lovePhoto.setClassify(resourcePathVO.getClassify());
        lovePhoto.setTitle(resourcePathVO.getTitle());
        lovePhoto.setCover(resourcePathVO.getCover());
        lovePhoto.setRemark(PoetryUtil.getUserId().toString());
        lovePhoto.setType(CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO);
        lovePhoto.setStatus(Boolean.FALSE);
        resourcePathMapper.insert(lovePhoto);
        return PoetryResult.success();
    }
}
