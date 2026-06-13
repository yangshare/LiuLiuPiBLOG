package com.liuliupi.controller;


import com.liuliupi.aop.LoginCheck;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.aop.SaveCheck;
import com.liuliupi.service.UserService;
import com.liuliupi.constants.CommonConst;
import com.liuliupi.utils.cache.PoetryCache;
import com.liuliupi.utils.PoetryUtil;
import com.liuliupi.vo.CaptchaVO;
import com.liuliupi.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;


    /**
     * 用户名/密码注册
     */
    @PostMapping("/regist")
    public PoetryResult<UserVO> regist(@Validated @RequestBody UserVO user) {
        return userService.regist(user);
    }


    /**
     * 获取图形验证码（公开接口，进入登录页即可调用，无需登录）
     */
    @GetMapping("/captcha")
    public PoetryResult<CaptchaVO> captcha() {
        return userService.captcha();
    }


    /**
     * 用户名、邮箱、手机号/密码登录
     * 统一 Token 体系，不再区分前台/后台
     */
    @PostMapping("/login")
    public PoetryResult<UserVO> login(@RequestParam("account") String account,
                                      @RequestParam("password") String password) {
        return userService.login(account, password);
    }


    /**
     * Token登录
     */
    @PostMapping("/token")
    public PoetryResult<UserVO> login(@RequestParam("userToken") String userToken) {
        return userService.token(userToken);
    }


    /**
     * 退出
     */
    @GetMapping("/logout")
    @LoginCheck
    public PoetryResult exit() {
        return userService.exit();
    }


    /**
     * 更新用户信息
     */
    @PostMapping("/updateUserInfo")
    @LoginCheck
    public PoetryResult<UserVO> updateUserInfo(@RequestBody UserVO user) {
        PoetryCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userService.updateUserInfo(user);
    }

    /**
     * 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCode")
    @LoginCheck
    @SaveCheck
    public PoetryResult getCode(@RequestParam("flag") Integer flag) {
        return userService.getCode(flag);
    }

    /**
     * 绑定手机号或者邮箱
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForBind")
    @LoginCheck
    @SaveCheck
    public PoetryResult getCodeForBind(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userService.getCodeForBind(place, flag);
    }

    /**
     * 更新邮箱、手机号、密码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 3 密码：place=老密码&password=新密码
     */
    @PostMapping("/updateSecretInfo")
    @LoginCheck
    public PoetryResult<UserVO> updateSecretInfo(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam(value = "code", required = false) String code, @RequestParam("password") String password) {
        PoetryCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userService.updateSecretInfo(place, flag, code, password);
    }

    /**
     * 忘记密码 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForForgetPassword")
    @SaveCheck
    public PoetryResult getCodeForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userService.getCodeForForgetPassword(place, flag);
    }

    /**
     * 忘记密码 更新密码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @PostMapping("/updateForForgetPassword")
    public PoetryResult updateForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam("code") String code, @RequestParam("password") String password) {
        return userService.updateForForgetPassword(place, flag, code, password);
    }

    /**
     * 根据用户名查找用户信息
     */
    @GetMapping("/getUserByUsername")
    @LoginCheck
    public PoetryResult<List<UserVO>> getUserByUsername(@RequestParam("username") String username) {
        return userService.getUserByUsername(username);
    }

    /**
     * 订阅/取消订阅专栏（标签）
     * <p>
     * flag = true：订阅
     * flag = false：取消订阅
     */
    @GetMapping("/subscribe")
    @LoginCheck
    public PoetryResult<UserVO> subscribe(@RequestParam("labelId") Integer labelId, @RequestParam("flag") Boolean flag) {
        PoetryCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userService.subscribe(labelId, flag);
    }
}

