package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统用户表 sys_user，用于简单登录。
 */
@Entity
@Table(name = "sys_user")
@Getter
@Setter
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String username;

    @Column(nullable = false, length = 64)
    private String password;

    /** 1 启用 0 禁用 */
    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
