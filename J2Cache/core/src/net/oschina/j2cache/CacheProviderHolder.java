/**
 * Copyright (c) 2015-2017, Winter Lau (javayou@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oschina.j2cache;

import net.oschina.j2cache.caffeine.CaffeineProvider;
import net.oschina.j2cache.ehcache.EhCacheProvider3;
import net.oschina.j2cache.redis.ReadonlyRedisCacheProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.oschina.j2cache.ehcache.EhCacheProvider;
import net.oschina.j2cache.redis.RedisCacheProvider;

import java.util.Collection;

/**
 * 两级的缓存管理器
 */
public class CacheProviderHolder {

	private final static Logger log = LoggerFactory.getLogger(CacheProviderHolder.class);

	private static CacheProvider l1_provider;
	private static CacheProvider l2_provider;

	private static CacheExpiredListener listener;

	/**
	 * Initialize Cache Provider
	 * 初始化缓存配置
	 * @param config j2cache config instance
	 * @param listener cache listener
	 */
	public static void init(J2CacheConfig config, CacheExpiredListener listener){
		//设置过期缓存监听器
		CacheProviderHolder.listener = listener;
		//生成一级缓存实现类
		CacheProviderHolder.l1_provider = loadProviderInstance(config.getL1CacheName());
		if (!l1_provider.isLevel(CacheObject.LEVEL_1)) {
			throw new CacheException(l1_provider.getClass().getName() + " is not level_1 cache provider");
		}
		//启动一级缓存
		CacheProviderHolder.l1_provider.start(config.getL1CacheProperties());
		log.info("Using L1 CacheProvider : " + l1_provider.getClass().getName());
		//生成二级缓存实现类
		CacheProviderHolder.l2_provider = loadProviderInstance(config.getL2CacheName());
		if (!l2_provider.isLevel(CacheObject.LEVEL_2)) {
			throw new CacheException(l2_provider.getClass().getName() + " is not level_2 cache provider");
		}
		//启动二级缓存
		CacheProviderHolder.l2_provider.start(config.getL2CacheProperties());
		log.info("Using L2 CacheProvider : " + l2_provider.getClass().getName());
	}

	/**
	 * 关闭缓存
	 */
	public final static void shutdown() {
		l1_provider.stop();
		l2_provider.stop();
	}

	//生成缓存具体实现类
	private final static CacheProvider loadProviderInstance(String cacheIdent) {
		if("ehcache".equalsIgnoreCase(cacheIdent)) {
			return new EhCacheProvider();
		}
		if("ehcache3".equalsIgnoreCase(cacheIdent)) {
			return new EhCacheProvider3();
		}
		if("caffeine".equalsIgnoreCase(cacheIdent)) {
			return new CaffeineProvider();
		}
		if("redis".equalsIgnoreCase(cacheIdent)) {
			return new RedisCacheProvider();
		}
		if("readonly-redis".equalsIgnoreCase(cacheIdent)) {
			return new ReadonlyRedisCacheProvider();
		}
		if("none".equalsIgnoreCase(cacheIdent)) {
			return new NullCacheProvider();
		}
		try {
			return (CacheProvider) Class.forName(cacheIdent).newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new CacheException("Failed to initialize cache providers", e);
		}
	}

	//返回一级缓存实现类
	public final static CacheProvider getL1Provider() {
		return l1_provider;
	}

	//返回二级缓存实现类
	public final static CacheProvider getL2Provider() {
		return l2_provider;
	}

	/**
	 * 一级缓存实例
	 * @param region  cache region
	 * @return level 1 cache instance
	 */
	public final static Level1Cache getLevel1Cache(String region) {
		return (Level1Cache)l1_provider.buildCache(region, listener);
	}

	/**
	 * 一级缓存实例
	 * @param region  cache region
	 * @param timeToLiveSeconds  cache ttl
	 * @return level 1 cache instance
	 */
	public final static Level1Cache getLevel1Cache(String region, long timeToLiveSeconds) {
		return (Level1Cache)l1_provider.buildCache(region, timeToLiveSeconds, listener);
	}

	/**
	 * 二级缓存实例
	 * @param region cache region
	 * @return level 2 cache instance
	 */
	public final static Level2Cache getLevel2Cache(String region) {
		return (Level2Cache)l2_provider.buildCache(region, listener);
	}

	/**
	 * return all regions
	 * 返回所有分区
	 * @return all regions
	 */
	public final static Collection<CacheChannel.Region> regions() {
		return l1_provider.regions();
	}

}
