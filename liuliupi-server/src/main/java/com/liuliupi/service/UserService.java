package com.liuliupi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.entity.User;
import com.liuliupi.vo.BaseRequestVO;
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
     * 统一 Token 体系，不再区分前台/后台登录
     *
     * @param account 账号（用户名/邮箱/手机号）
     * @param password 密码（加密后）
     * @return 用户信息（含 Token）
     */
    PoetryResult<UserVO> login(String account, String password);

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
