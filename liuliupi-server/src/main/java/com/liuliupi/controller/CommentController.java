package com.liuliupi.controller;


import com.liuliupi.aop.LoginCheck;
import com.liuliupi.config.PoetryResult;
import com.liuliupi.aop.SaveCheck;
import com.liuliupi.service.CommentService;
import com.liuliupi.constants.CommonConst;
import com.liuliupi.utils.CommonQuery;
import com.liuliupi.utils.cache.PoetryCache;
import com.liuliupi.utils.StringUtil;
import com.liuliupi.vo.BaseRequestVO;
import com.liuliupi.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 文章评论表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/comment")
public class CommentController {


    @Autowired
    private CommentService commentService;

    @Autowired
    private CommonQuery commonQuery;


    /**
     * 保存评论
     */
    @PostMapping("/saveComment")
    @LoginCheck
    @SaveCheck
    public PoetryResult saveComment(@Validated @RequestBody CommentVO commentVO) {
        String content = StringUtil.removeHtml(commentVO.getCommentContent());
        if (!StringUtils.hasText(content)) {
            return PoetryResult.fail("评论内容不合法！");
        }
        commentVO.setCommentContent(content);

        PoetryCache.remove(CommonConst.COMMENT_COUNT_CACHE + commentVO.getSource().toString() + "_" + commentVO.getType());
        return commentService.saveComment(commentVO);
    }


    /**
     * 删除评论
     */
    @GetMapping("/deleteComment")
    @LoginCheck
    public PoetryResult deleteComment(@RequestParam("id") Integer id) {
        return commentService.deleteComment(id);
    }


    /**
     * 查询评论数量
     */
    @GetMapping("/getCommentCount")
    public PoetryResult<Integer> getCommentCount(@RequestParam("source") Integer source, @RequestParam("type") String type) {
        return PoetryResult.success(commonQuery.getCommentCount(source, type));
    }


    /**
     * 查询评论
     */
    @PostMapping("/listComment")
    public PoetryResult<BaseRequestVO> listComment(@RequestBody BaseRequestVO baseRequestVO) {
        return commentService.listComment(baseRequestVO);
    }
}

