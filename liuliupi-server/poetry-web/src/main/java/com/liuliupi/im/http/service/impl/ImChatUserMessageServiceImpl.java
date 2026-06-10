package com.liuliupi.im.http.service.impl;

import com.liuliupi.im.http.entity.ImChatUserMessage;
import com.liuliupi.im.http.dao.ImChatUserMessageMapper;
import com.liuliupi.im.http.service.ImChatUserMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 单聊记录 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@Service
public class ImChatUserMessageServiceImpl extends ServiceImpl<ImChatUserMessageMapper, ImChatUserMessage> implements ImChatUserMessageService {

}
