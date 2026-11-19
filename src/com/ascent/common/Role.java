package com.ascent.common;

/**
 * 用户角色。
 *
 * <p>提示：角色只在服务端判断，客户端传上来的 role 一律不可信。
 * 数据库里存字符串（USER / ADMIN），DTO 里用枚举，避免魔法值。</p>
 */
public enum Role {
    USER,
    ADMIN
}
