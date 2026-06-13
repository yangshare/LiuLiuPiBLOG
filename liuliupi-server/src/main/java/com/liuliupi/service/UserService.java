package com.liuliupi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.entity.User;
import com.liuliupi.vo.BaseRequestVO;
import com.liuliupi.vo.CaptchaVO;
import com.liuliupi.vo.UserVO;

import java.util.List;

/**
 * <p>
 * 用户信息表 服务类
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
public interface UserService extends IService<User> {

    /**
     * 用户名、邮箱、手机号/密码登录
     * 统一 Token 体系，不再区分前后台登录
     *
     * @param account      账号（用户名/邮箱/手机号）
     * @param password     密码（AES 加密后）
     * @param captchaToken 图形验证码 token（来自 GET /user/captcha）
     * @param code         用户输入的图形验证码
     * @return 用户信息（含 Token）
     */
    PoetryResult<UserVO> login(String account, String password, String captchaToken, String code);

    /**
     * 生成图形验证码（公开接口，无需登录）
     *
     * @return 含 captchaToken 与 base64 图片的 VO
     */
    PoetryResult<CaptchaVO> captcha();

    PoetryResult exit();

    PoetryResult<UserVO> regist(UserVO user);

    PoetryResult<UserVO> updateUserInfo(UserVO user);

    PoetryResult getCode(Integer flag);

    PoetryResult getCodeForBind(String place, Integer flag);

    PoetryResult<UserVO> updateSecretInfo(String place, Integer flag, String code, String password);

    PoetryResult getCodeForForgetPassword(String place, Integer flag);

    PoetryResult updateForForgetPassword(String place, Integer flag, String code, String password);

    PoetryResult<Page> listUser(BaseRequestVO baseRequestVO);

    PoetryResult<List<UserVO>> getUserByUsername(String username);

    PoetryResult<UserVO> token(String userToken);

    PoetryResult<UserVO> subscribe(Integer labelId, Boolean flag);
}
