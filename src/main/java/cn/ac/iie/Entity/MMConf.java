package cn.ac.iie.Entity;

import java.util.Set;

public class MMConf {
	private redis.clients.jedis.HostAndPort hap;
	private Set<String> sentinels;
	private RedisMode redisMode;

	public static enum RedisMode {
		STANDALONE, SENTINEL, CLUSTER;
	}

	
	private int redisTimeout = 30000;

	private boolean rpsUseCache = false;

	public Set<String> getSentinels() {
		return this.sentinels;
	}

	public void setSentinels(Set<String> sentinels) {
		this.sentinels = sentinels;
	}

	public RedisMode getRedisMode() {
		return this.redisMode;
	}

	public void setRedisMode(RedisMode redisMode) {
		this.redisMode = redisMode;
	}

	public int getRedisTimeout() {
		return this.redisTimeout;
	}

	public void setRedisTimeout(int redisTimeout) {
		this.redisTimeout = redisTimeout;
	}

	public redis.clients.jedis.HostAndPort getHap() {
		return this.hap;
	}

	public void setHap(redis.clients.jedis.HostAndPort hap) {
		this.hap = hap;
	}

	public boolean isRpsUseCache() {
		return this.rpsUseCache;
	}

	public void setRpsUseCache(boolean rpsUseCache) {
		this.rpsUseCache = rpsUseCache;
	}
}
