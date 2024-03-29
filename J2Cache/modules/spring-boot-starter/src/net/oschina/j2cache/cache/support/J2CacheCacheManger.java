package net.oschina.j2cache.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.util.CollectionUtils;

import net.oschina.j2cache.CacheChannel;


/**
 * {@link Cache} implementation for J2Cache.
 *  本地缓存管理类
 *
 */
public class J2CacheCacheManger extends AbstractTransactionSupportingCacheManager{
	
	private boolean allowNullValues = true;
	
	private Collection<String> cacheNames;
	
	private boolean dynamic = true;
	
	private CacheChannel cacheChannel;
	
	public J2CacheCacheManger(CacheChannel cacheChannel){
		this.cacheChannel = cacheChannel;
	}

	/**
	 * 批量导入本地缓存
	 * @return
	 */
	@Override
	protected Collection<? extends Cache> loadCaches() {
		Collection<Cache> caches = new LinkedHashSet<>(cacheNames.size());
		for (String name : cacheNames) {
			J2CacheCache cache = new J2CacheCache(name, cacheChannel, allowNullValues);
			caches.add(cache);
		}
		return caches;
	}
	
	
	public boolean isAllowNullValues() {
		return allowNullValues;
	}

	public void setAllowNullValues(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}
	
	@Override
	protected Cache getMissingCache(String name) {
		return this.dynamic ? new J2CacheCache(name, cacheChannel, allowNullValues) : null;
	}

	/**
	 * 设置本地缓存
	 * @param cacheNames
	 */
	public void setCacheNames(Collection<String> cacheNames) {
		Set<String> newCacheNames = CollectionUtils.isEmpty(cacheNames) ? Collections.<String> emptySet()
				: new HashSet<String>(cacheNames);
		this.cacheNames = newCacheNames;
		this.dynamic = newCacheNames.isEmpty();
	}

}
