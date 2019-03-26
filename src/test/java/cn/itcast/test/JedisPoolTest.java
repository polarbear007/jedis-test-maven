package cn.itcast.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;

public class JedisPoolTest {
	private Jedis jedis;
	
	@Before
	public void before() {
		jedis = JedisPoolUtils.getJedisInstance();
	}
	
	@After
	public void after() {
		// 跟c3p0 一样，我们不用担心这个 close() 方法会销毁连接对象
		// 这个方法肯定被 JedisPool 重写过，当我们执行些方法时，会返回连接池对象，等待再次被使用
		jedis.close();
	}
	
	// 没有什么好写了，直接就放个空方法，知道个流程就好了
	@Test
	public void test() {
		
	}
}
