package com.liuliupi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 首页弹窗推送配置表
 * </p>
 *
 * @author sara
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("push_notification")
public class PushNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 推送标题
     */
    @TableField("title")
    private String title;

    /**
     * 封面图 URL
     */
    @TableField("cover")
    private String cover;

    /**
     * 点击跳转链接
     */
    @TableField("url")
    private String url;

    /**
     * 是否启用[0:否，1:是]
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
