package net.oschina.j2cache.cache.support.redis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import net.oschina.j2cache.ClusterPolicy;
import net.oschina.j2cache.Command;
import net.oschina.j2cache.J2CacheConfig;
import net.oschina.j2cache.cache.support.util.SpringUtil;

/**
 * 使用spring redis实现订阅功能
 *
 */
public class SpringRedisPubSubPolicy implements ClusterPolicy{
	
	private RedisTemplate<String, Serializable> redisTemplate;
	
	private net.oschina.j2cache.autoconfigure.J2CacheConfig config;
	
	/**
	 * 是否是主动模式
	 */
	private static boolean isActive = false;
	
	private String channel = "j2cache_channel";
	
	@SuppressWarnings("unchecked")
	@Override
	public void connect(Properties props) {
		//获取配置bean类
		J2CacheConfig j2config = SpringUtil.getBean(J2CacheConfig.class);
		this.config =  SpringUtil.getBean(net.oschina.j2cache.autoconfigure.J2CacheConfig.class);
		this.redisTemplate = SpringUtil.getBean("j2CacheRedisTemplate", RedisTemplate.class);
		if("active".equals(config.getCacheCleanMode())) {
			isActive = true;
		}
		String channel_name = j2config.getL2CacheProperties().getProperty("channel");
		if(channel_name != null && !channel_name.isEmpty()) {
			this.channel = channel_name;
		}
		RedisMessageListenerContainer listenerContainer = SpringUtil.getBean("j2CacheRedisMessageListenerContainer", RedisMessageListenerContainer.class);
		
		listenerContainer.addMessageListener(new SpringRedisMessageListener(this, this.channel), new PatternTopic(this.channel));
		if(isActive) {
			//设置键值回调
			ConfigureNotifyKeyspaceEventsAction action = new ConfigureNotifyKeyspaceEventsAction();
			action.config(listenerContainer.getConnectionFactory().getConnection());
			
			String namespace = 	j2config.getL2CacheProperties().getProperty("namespace");
			String database = j2config.getL2CacheProperties().getProperty("database");
			String expired  = "__keyevent@" + (database == null || "".equals(database) ? "0" : database) + "__:expired";
			String del = "__keyevent@" + (database == null || "".equals(database) ? "0" : database) + "__:del";
			List<PatternTopic> topics = new ArrayList<>();
			topics.add(new PatternTopic(expired));
			topics.add(new PatternTopic(del));	
			listenerContainer.addMessageListener(new SpringRedisActiveMessageListener(this, namespace), topics);
		}

	}

	@Override
	public void sendEvictCmd(String region, String... keys) {
		//当缓存配置中设置为非活动状态 或清理模式为混合模式 则发送删除命令
		if(!isActive || "blend".equals(config.getCacheCleanMode())) {
			String com = new Command(Command.OPT_EVICT_KEY, region, keys).json();
	        redisTemplate.convertAndSend(this.channel, com);	
		}

	}

	@Override
	public void sendClearCmd(String region) {
		//当缓存配置中设置为非活动状态 或清理模式为混合模式 则发送清理命令
		if(!isActive || "blend".equals(config.getCacheCleanMode())) {
			String com = new Command(Command.OPT_CLEAR_KEY, region, "").json();
			redisTemplate.convertAndSend(this.channel, com);	
		}
	}

	@Override
	public void disconnect() {
		redisTemplate.convertAndSend(this.channel, Command.quit().json());
	}

	
}
