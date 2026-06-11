package com.liuliupi.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.liuliupi.aop.LoginCheck;
import com.liuliupi.aop.SaveCheck;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.constants.CommonConst;
import com.liuliupi.dao.TreeHoleMapper;
import com.liuliupi.entity.TreeHole;
import com.liuliupi.utils.PoetryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

/**
 * <p>
 * 弹幕 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class TreeHoleController {

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    /**
     * 保存
     */
    @PostMapping("/saveTreeHole")
    @SaveCheck
    public PoetryResult<TreeHole> saveTreeHole(@RequestBody TreeHole treeHole) {
        if (!StringUtils.hasText(treeHole.getMessage())) {
            return PoetryResult.fail("留言不能为空！");
        }
        treeHoleMapper.insert(treeHole);
        if (!StringUtils.hasText(treeHole.getAvatar())) {
            treeHole.setAvatar(PoetryUtil.getRandomAvatar(null));
        }
        return PoetryResult.success(treeHole);
    }


    /**
     * 删除
     */
    @GetMapping("/deleteTreeHole")
    @LoginCheck(0)
    public PoetryResult deleteTreeHole(@RequestParam("id") Integer id) {
        treeHoleMapper.deleteById(id);
        return PoetryResult.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listTreeHole")
    public PoetryResult<List<TreeHole>> listTreeHole() {
        List<TreeHole> treeHoles;
        Integer count = new LambdaQueryChainWrapper<>(treeHoleMapper).count();
        if (count > CommonConst.TREE_HOLE_COUNT) {
            int i = new Random().nextInt(count + 1 - CommonConst.TREE_HOLE_COUNT);
            treeHoles = treeHoleMapper.queryAllByLimit(i, CommonConst.TREE_HOLE_COUNT);
        } else {
            treeHoles = new LambdaQueryChainWrapper<>(treeHoleMapper).list();
        }

        treeHoles.forEach(treeHole -> {
            if (!StringUtils.hasText(treeHole.getAvatar())) {
                treeHole.setAvatar(PoetryUtil.getRandomAvatar(treeHole.getId().toString()));
            }
        });
        return PoetryResult.success(treeHoles);
    }
}
