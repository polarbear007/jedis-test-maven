package cn.itcast.test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

// jedis 本身自带了连接池工具，不需要我们去配置什么c3p0 什么的
// 但是这个连接池一个项目应该是单例的，如果我们整合了 spring 的话，那么好办
// 直接在spring 中配置这个连接池对象就好了。 但是如果我们没有跟spring 整合的话
// 那么就得自己来写一个单例工厂，获取jedisPool 连接池对象

public class JedisPoolUtils {
	// 单例的最基本要求，私有化构造方法
	private JedisPoolUtils() {}
	
	// 提供一个静态的 jedisPool 对象
	// 创建连接池对象的时候，我们可以提供一个配置对象，这个配置对象可以配置连接池参数
	// 但是如果我们什么都不想配置的话，可以直接new 一个，什么都使用默认配置
	// 甚至，你创建 jedisPool 的时候，都可以只传 ip 和 端口号
	private static JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "192.168.48.128", 6379);

	// 建好了静态的连接池对象以后，我们要提供一个静态方法来获取这个实例
	public static JedisPool getJedisPoolInstance() {
		return jedisPool;
	}
	
	// 当然，我们连连接池对象都不想对外暴露，直接拿到连接对象  jedis 也是可以的
	public static Jedis getJedisInstance() {
		return jedisPool.getResource();
	}
}
