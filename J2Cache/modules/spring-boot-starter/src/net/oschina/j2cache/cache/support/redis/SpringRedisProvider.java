package net.oschina.j2cache.cache.support.redis;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.RedisTemplate;

import net.oschina.j2cache.Cache;
import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheExpiredListener;
import net.oschina.j2cache.CacheObject;
import net.oschina.j2cache.CacheProvider;
import net.oschina.j2cache.cache.support.util.SpringUtil;

/**
 * spring redis缓存
 *
 */

public class SpringRedisProvider implements CacheProvider {

	private RedisTemplate<String, Serializable> redisTemplate;

	private String namespace;

	private String storage;
	
	protected ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<>();

	@Override
	public String name() {
		return "redis";
	}

	@Override
	public int level() {
		return CacheObject.LEVEL_2;
	}

	@Override
	public Collection<CacheChannel.Region> regions() {
		return Collections.emptyList();
	}

	/**
	 *  生成缓存操作类
	 * @param region
	 * @param listener listener for expired elements
	 * @return
	 */
	@Override
	public Cache buildCache(String region, CacheExpiredListener listener) {
		Cache cache = caches.get(region);
		if (cache == null) {
			synchronized (SpringRedisProvider.class) {
				cache = caches.get(region);
				if (cache == null) {
	                if("hash".equalsIgnoreCase(this.storage))
	                    cache = new SpringRedisCache(this.namespace, region, redisTemplate);
	                else {
	                	cache = new SpringRedisGenericCache(this.namespace, region, redisTemplate);
					}
					caches.put(region, cache);
				}
			}
		}
		return cache;
	}

	@Override
	public Cache buildCache(String region, long timeToLiveInSeconds, CacheExpiredListener listener) {
		return buildCache(region, listener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void start(Properties props) {
		this.namespace = props.getProperty("namespace");
		this.storage = props.getProperty("storage");
		this.redisTemplate = SpringUtil.getBean("j2CacheRedisTemplate", RedisTemplate.class);
	}

	@Override
	public void stop() {
		// 由spring控制
	}

}
