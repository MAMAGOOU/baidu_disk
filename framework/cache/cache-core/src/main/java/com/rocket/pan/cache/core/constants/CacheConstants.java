package com.rocket.pan.cache.core.constants;

/**
 * R Pan 服务端公用缓存常量类
 *
 * @author 19750
 * @version 1.0
 */
public interface CacheConstants {
    /**
     * R Pan 服务端公用缓存名称
     * 由于该缓存框架大部分复用Spring的Cache模块，所以使用统一的缓存名称
     */
    String R_PAN_CACHE_NAME = "R_PAN_CACHE";
}
