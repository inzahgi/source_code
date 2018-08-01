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

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cache Channel, the J2Cache methods explored to developers
 * 缓存操作 对外接口
 */
public abstract class CacheChannel implements Closeable , AutoCloseable {

	private static final Map<String, Object> _g_keyLocks = new ConcurrentHashMap<>();
	//缓存配置参数类
	private J2CacheConfig config;
	//缓存为空标志位
    private boolean defaultCacheNullObject ;

    // 初始化配置参数
	public CacheChannel(J2CacheConfig config) {
		this.config = config;
		this.defaultCacheNullObject = config.isDefaultCacheNullObject();
	}

	private NullObject newNullObject() {
		return new NullObject();
	}

	/**
	 * <p>Just for Inner Use.</p>
	 *
	 * <p>To clear the whole region when received this event .</p>
	 * 抽象类方法 清空分区内的所有缓存
	 * @param region Cache region name
	 */
	protected abstract void sendClearCmd(String region);

	/**
	 * <p>Just for Inner Use.</p>
	 *
	 * <p>To remove cached data when received this event .</p>
	 *  抽象类方法 删除指定分区中的key
	 * @param region Cache region name
	 * @param keys	Cache data key
	 */
	protected abstract void sendEvictCmd(String region, String...keys);

	/**
	 * 读取缓存（用户无需判断返回的对象是否为空）
	 * @param region Cache region name
	 * @param key Cache data key
     * @param cacheNullObject 是否缓存空对象
	 * @return cache object
	 */
	public CacheObject get(String region, String key, boolean...cacheNullObject)  {
		//生成缓存的封装参数类
		CacheObject obj = new CacheObject(region, key, CacheObject.LEVEL_1);
		//按照key 从L1缓存提供管理器获取相应的值
		obj.setValue(CacheProviderHolder.getLevel1Cache(region).get(key));
		if(obj.rawValue() != null) {
			return obj;
		}
		// 计算该key 全局的锁的key
		String lock_key = key + '%' + region;
		// 对该key 加全局锁
		synchronized (_g_keyLocks.computeIfAbsent(lock_key, v -> new Object())) {
			//判断是否有其他线程生成   当有一级缓存时直接返回
			obj.setValue(CacheProviderHolder.getLevel1Cache(region).get(key));
			if(obj.rawValue() != null) {
				return obj;
			}

			try {
				//设置缓存级别为二级缓存
				obj.setLevel(CacheObject.LEVEL_2);
				//从二级缓存获取中获取值
				obj.setValue(CacheProviderHolder.getLevel2Cache(region).get(key));
				if (obj.rawValue() != null) {
					//将二级缓存的值保存到一级缓存中
					CacheProviderHolder.getLevel1Cache(region).put(key, obj.rawValue());
				}else {
					//如果二级缓存为空， 则设置默认值
                    boolean cacheNull = (cacheNullObject.length>0)?cacheNullObject[0]: defaultCacheNullObject;
                    if(cacheNull) {
						set(region, key, newNullObject(), true);
					}
                }
			} finally {
				//移除全局锁
				_g_keyLocks.remove(lock_key);
			}
		}

		return obj;
	}

	/**
	 * 支持外部数据自动加载的缓存方法
	 * @param region Cache region name
	 * @param key Cache data key
	 * @param loader data loader
	 * @param cacheNullObject  true if you need to cache null object
	 * @return cache object
	 */
	public CacheObject get(String region, String key, Function<String, Object> loader, boolean...cacheNullObject) {
		//获取缓存 如果有值 直接返回
		CacheObject cache = get(region, key, false);

		if (cache.rawValue() != null) {
			return cache;
		}

		String lock_key = key + '@' + region;
		//设置全局锁
		synchronized (_g_keyLocks.computeIfAbsent(lock_key, v -> new Object())) {
			//其他线程设置缓存则返回
			cache = get(region, key, false);

			if (cache.rawValue() != null) {
				return cache;
			}

			try {
				//加载缓存值
				Object obj = loader.apply(key);
				boolean cacheNull = (cacheNullObject.length>0)?cacheNullObject[0]: defaultCacheNullObject;
				//设置缓存
				set(region, key, obj, cacheNull);
				//封装返回缓存类
				cache = new CacheObject(region, key, CacheObject.LEVEL_OUTER, obj);
			} finally {
				//解除全局锁
				_g_keyLocks.remove(lock_key);
			}
		}
		return cache;
	}

	/**
	 * 批量读取缓存中的对象（用户无需判断返回的对象是否为空）
	 * @param region Cache region name
	 * @param keys cache keys
	 * @return cache object
	 */
	public Map<String, CacheObject> get(String region, Collection<String> keys)  {
		//批量获取一级缓存值
		final Map<String, Object> objs = CacheProviderHolder.getLevel1Cache(region).get(keys);
		//过滤没有找到一级缓存的key
		List<String> level2Keys = keys
				.stream().filter(k -> !objs.containsKey(k) || objs.get(k) == null).collect(Collectors.toList());

		//映射需要查询到缓存的key 返回结果
		Map<String, CacheObject> results = objs.entrySet().stream()
										.filter(p -> p.getValue() != null).collect(
			Collectors.toMap(
				p -> p.getKey(),
				p -> new CacheObject(region, p.getKey(), CacheObject.LEVEL_1, p.getValue())
			)
		);
		//查找二级缓存
		Map<String, Object> objs_level2 = CacheProviderHolder.getLevel2Cache(region).get(level2Keys);
		//遍历查询二级缓存结果
		objs_level2.forEach((k,v) -> {
			//将其设置到返回结果集中
			results.put(k, new CacheObject(region, k, CacheObject.LEVEL_2, v));
			// 如果有缓存则设置一级缓存
			if (v != null) {
				CacheProviderHolder.getLevel1Cache(region).put(k, v);
			}
		});

		return results;
	}

	/**
	 * 使用数据加载器的批量缓存读取   引入外部数据时
	 * @param region Cache region name
	 * @param keys cache keys
	 * @param loader data loader
	 * @param cacheNullObject true if you need to cache null object
	 * @return multiple cache data
	 */
	public Map<String, CacheObject> get(String region, Collection<String> keys, Function<String, Object> loader, boolean...cacheNullObject)  {
		//批量获取缓存
		Map<String, CacheObject> results = get(region, keys);
		//先找出所有没有缓存的结果  然后加全局锁
		results.entrySet().stream().filter(e -> e.getValue().rawValue() == null).forEach( e -> {
			String lock_key = e.getKey() + '@' + region;
			synchronized (_g_keyLocks.computeIfAbsent(lock_key, v -> new Object())) {
				CacheObject cache = get(region, e.getKey(), false);
				if(cache.rawValue() == null) {
					try {
						//利用引入的加载器加载数据
						Object obj = loader.apply(e.getKey());
						// 获取缓存null object标志位
						boolean cacheNull = (cacheNullObject.length>0)?cacheNullObject[0]: defaultCacheNullObject;
						// 保存缓存
						set(region, e.getKey(), obj, cacheNull);
						//将缓存保存到返回结果中
						e.getValue().setValue(obj);
						// 设置返回缓存级别
						e.getValue().setLevel(CacheObject.LEVEL_OUTER);
					} finally {
						_g_keyLocks.remove(lock_key);
					}
				}
				else {
					e.setValue(cache);
				}
			}
		});
		return results;
	}

	/**
	 * 判断某个缓存键是否存在
	 * @param region Cache region name
	 * @param key cache key
	 * @return true if key exists
	 */
	public boolean exists(String region, String key) {
		return check(region, key) > 0;
	}

	/**
	 * 判断某个key存在于哪级的缓存中
	 * @param region cache region
	 * @param key cache key
	 * @return  0(不存在),1(一级),2(二级)
	 */
	public int check(String region, String key) {
		if(CacheProviderHolder.getLevel1Cache(region).exists(key)) {
			return 1;
		}
		if(CacheProviderHolder.getLevel2Cache(region).exists(key)) {
			return 2;
		}
		return 0;
	}

	/**
	 * Write data to J2Cache
	 * 将数据保存到缓存中
	 *
	 * @param region: Cache Region name
	 * @param key: Cache key
	 * @param value: Cache value
	 */
	public void set(String region, String key, Object value) {
		set(region, key, value, defaultCacheNullObject);
	}

	/**
	 * Write data to J2Cache
	 *
	 * @param region: Cache Region name
	 * @param key: Cache key
	 * @param value: Cache value
	 * @param cacheNullObject if allow cache null object
	 */
	public void set(String region, String key, Object value, boolean cacheNullObject) {
		//设置默认缓存标志为false 并且值未null  直接返回
		if (!cacheNullObject && value == null) {
			return;
		}

		try {
			// 获取一级缓存的缓存实现类
			Level1Cache level1 = CacheProviderHolder.getLevel1Cache(region);
			//  设置一级缓存
			level1.put(key, (value==null && cacheNullObject)?newNullObject():value);
			// 获取二级缓存的操作类
			Level2Cache level2 = CacheProviderHolder.getLevel2Cache(region);
			// 判断是否需要同步过期时间 设置二级缓存
			if(config.isSyncTtlToRedis()) {
				level2.put(key, (value == null && cacheNullObject) ? newNullObject() : value, level1.ttl());
			}else {
				level2.put(key, (value == null && cacheNullObject) ? newNullObject() : value);
			}
		} finally {
			//todo //
			// 这里清除一级缓存  下次加载设置时就可以直接设置新缓存了  ？？？？
			this.sendEvictCmd(region, key);//清除原有的一级缓存的内容
		}
    }


	/**
	 * Write data to j2cache with expired setting
	 *
	 * <strong>注意：强烈不推荐使用带 TTL 的 set 方法，所有的缓存 TTL 都应该预先配置好，避免多个节点的缓存 Region 配置不同步</strong>
	 *
	 * @param region Cache Region name
	 * @param key Cache Key
	 * @param value Cache value
	 * @param timeToLiveInSeconds cache expired in second
	 */
	public void set(String region, String key, Object value, long timeToLiveInSeconds ) {
		set(region, key, value, timeToLiveInSeconds, defaultCacheNullObject);
	}

	/**
	 * Write data to j2cache with expired setting
	 *
	 * <strong>注意：强烈不推荐使用带 TTL 的 set 方法，所有的缓存 TTL 都应该预先配置好，避免多个节点的缓存 Region 配置不同步</strong>
	 *
	 * @param region Cache Region name
	 * @param key Cache Key
	 * @param value Cache value
	 * @param timeToLiveInSeconds cache expired in second
	 * @param cacheNullObject if allow cache null object
	 */
    public void set(String region, String key, Object value, long timeToLiveInSeconds, boolean cacheNullObject) {
		if (!cacheNullObject && value == null) {
			return;
		}
		// 如果没有 过期时间 直接设置缓存
    	if(timeToLiveInSeconds <= 0) {
			set(region, key, value, cacheNullObject);
		}
    	else {
			try {
				// 设置一级缓存
				CacheProviderHolder.getLevel1Cache(region, timeToLiveInSeconds).put(key, (value==null && cacheNullObject)?newNullObject():value);
				// 获取二级缓存操作类
				Level2Cache level2 = CacheProviderHolder.getLevel2Cache(region);
				// 同步二级缓存到redis
				if(config.isSyncTtlToRedis()) {
					level2.put(key, (value == null && cacheNullObject) ? newNullObject() : value, timeToLiveInSeconds);
				}else {
					level2.put(key, (value == null && cacheNullObject) ? newNullObject() : value);
				}
			} finally {
				this.sendEvictCmd(region, key);//清除原有的一级缓存的内容
			}
		}
	}

	/**
	 * 批量插入数据
	 * @param region Cache Region name
	 * @param elements Cache Elements
	 */
	public void set(String region, Map<String, Object> elements){
    	set(region, elements, defaultCacheNullObject);
	}

	/**
	 * 批量插入数据
	 * @param region Cache Region name
	 * @param elements Cache Elements
	 * @param cacheNullObject if allow cache null object
	 */
	public void set(String region, Map<String, Object> elements, boolean cacheNullObject)  {
		try {
			// 插入为null的缓存
			if (cacheNullObject && elements.containsValue(null)) {
				Map<String, Object> newElems = new HashMap<>();
				newElems.putAll(elements);
				//遍历为空的值 插入默认空缓存
				newElems.forEach((k,v) -> {
					if (v == null) {
						newElems.put(k, newNullObject());
					}
				});
				//设置一级缓存
				Level1Cache level1 = CacheProviderHolder.getLevel1Cache(region);
				level1.put(newElems);
				//同步redis
				if(config.isSyncTtlToRedis()) {
					CacheProviderHolder.getLevel2Cache(region).put(newElems, level1.ttl());
				}else {
					CacheProviderHolder.getLevel2Cache(region).put(newElems);
				}
			}else {
				//有数据时 批量缓存 同步redis
				Level1Cache level1 = CacheProviderHolder.getLevel1Cache(region);
				level1.put(elements);
				if(config.isSyncTtlToRedis()) {
					CacheProviderHolder.getLevel2Cache(region).put(elements, level1.ttl());
				}else {
					CacheProviderHolder.getLevel2Cache(region).put(elements);
				}
			}
		} finally {
			//广播  删除一级缓存
			this.sendEvictCmd(region, elements.keySet().stream().toArray(String[]::new));
		}
	}

	/**
	 * 带失效时间的批量缓存数据插入
	 *
	 * <strong>注意：强烈不推荐使用带 TTL 的 set 方法，所有的缓存 TTL 都应该预先配置好，避免多个节点的缓存 Region 配置不同步</strong>
	 *
	 * @param region Cache Region name
	 * @param elements Cache Elements
	 * @param timeToLiveInSeconds cache expired in second
	 */
	public void set(String region, Map<String, Object> elements, long timeToLiveInSeconds){
		set(region, elements, timeToLiveInSeconds, defaultCacheNullObject);
	}

	/**
	 * 带失效时间的批量缓存数据插入
	 *
	 * <strong>注意：强烈不推荐使用带 TTL 的 set 方法，所有的缓存 TTL 都应该预先配置好，避免多个节点的缓存 Region 配置不同步</strong>
	 *
	 * @param region Cache Region name
	 * @param elements Cache Elements
	 * @param timeToLiveInSeconds cache expired in second
	 * @param cacheNullObject if allow cache null object
	 */
	public void set(String region, Map<String, Object> elements, long timeToLiveInSeconds, boolean cacheNullObject)  {
		// 无有效过期时间时， 直接设置缓存
		if(timeToLiveInSeconds <= 0) {
			set(region, elements, cacheNullObject);
		}else {
			try {
				// 如果存在null值时 根据设置 加入
				if (cacheNullObject && elements.containsValue(null)) {
					Map<String, Object> newElems = new HashMap<>();
					newElems.putAll(elements);
					newElems.forEach((k,v) -> {
						if (v == null) {
							newElems.put(k, newNullObject());
						}
					});
					CacheProviderHolder.getLevel1Cache(region, timeToLiveInSeconds).put(newElems);
					if(config.isSyncTtlToRedis()) {
						CacheProviderHolder.getLevel2Cache(region).put(newElems, timeToLiveInSeconds);
					}else {
						CacheProviderHolder.getLevel2Cache(region).put(newElems);
					}
				}
				else {
					//有正常数据时 保存缓存
					CacheProviderHolder.getLevel1Cache(region, timeToLiveInSeconds).put(elements);
					//同步到redis
					if(config.isSyncTtlToRedis()) {
						CacheProviderHolder.getLevel2Cache(region).put(elements, timeToLiveInSeconds);
					}else {
						CacheProviderHolder.getLevel2Cache(region).put(elements);
					}
				}
			} finally {
				//广播
				this.sendEvictCmd(region, elements.keySet().stream().toArray(String[]::new));
			}
		}
	}

	/**
	 * Remove cached data in J2Cache
	 * 删除缓存
	 * @param region:  Cache Region name
	 * @param keys: Cache key
	 */
	public void evict(String region, String...keys)  {
		try {
			//删除一级缓存
			CacheProviderHolder.getLevel1Cache(region).evict(keys);
			//删除二级缓存
			CacheProviderHolder.getLevel2Cache(region).evict(keys);
		} finally {
			this.sendEvictCmd(region, keys); //发送广播
		}
    }

	/**
	 * Clear the cache
	 *	清空分区下所有缓存
	 * @param region: Cache region name
	 */
	public void clear(String region)  {
		try {
			//清空该分区下一级缓存
			CacheProviderHolder.getLevel1Cache(region).clear();
			//清空该分区下的二级缓存
			CacheProviderHolder.getLevel2Cache(region).clear();
		}finally {
			this.sendClearCmd(region);
		}
    }

	/**
	 * 返回所有的缓存区域
	 * @return all the regions
	 */
	public Collection<Region> regions() {
		return CacheProviderHolder.regions();
	}

	/**
	 * <p>Get cache region keys</p>
	 * <p><strong>Notice: ehcache3 not support keys</strong></p>
	 *
	 * 获取该分区下所有的缓存键
	 * @param region: Cache region name
	 * @return key list
	 */
	public Collection<String> keys(String region)  {
		Set<String> keys = new HashSet<>();
		keys.addAll(CacheProviderHolder.getLevel1Cache(region).keys());
		keys.addAll(CacheProviderHolder.getLevel2Cache(region).keys());
		return keys;
    }

	/**
	 * Close J2Cache
	 */
	@Override
	public abstract void close();

	/**
	 * 获取一级缓存接口
	 * @return 返回一级缓存的 CacheProvider 实例
	 */
	public CacheProvider getL1Provider() {
		return CacheProviderHolder.getL1Provider();
	}

	/**
	 * <p>获取二级缓存的接口，该方法可用来直接获取 J2Cache 底层的 Redis 客户端实例</p>
	 * <p>方法如下：</p>
	 * <code>
	 *     CacheChannel channel = J2Cache.getChannel();
	 *     RedisClient client = ((RedisCacheProvider)channel.getL2Provider()).getRedisClient();
	 *     try {
	 *     	   client.get().xxxxx(...);
	 *     } finally {
	 *         client.release();
	 *     }
	 * </code>
	 * @return 返回二级缓存的 CacheProvider 实例
	 */
	public CacheProvider getL2Provider() {
		return CacheProviderHolder.getL2Provider();
	}

	/**
	 * Cache Region Define
	 *  分区的结构
	 */
	public static class Region {

		private String name;
		private long size;
		private long ttl;

		public Region(String name, long size, long ttl) {
			this.name = name;
			this.size = size;
			this.ttl = ttl;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}

		public long getTtl() {
			return ttl;
		}

		public void setTtl(long ttl) {
			this.ttl = ttl;
		}

		@Override
		public String toString() {
			return String.format("[%s,size:%d,ttl:%d]", name, size, ttl);
		}
	}

}
