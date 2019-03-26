package cn.itcast.test;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class JedisPipeLineTest {
	private Jedis jedis;

	@Before
	public void before() {
		jedis = JedisPoolUtils.getJedisInstance();
	}

	@After
	public void after() {
		jedis.close();
	}

	// 注意： 管道的作用其实跟事务有点像，就是把命令先存起来，然后一次性发送，redis 数据库统一执行，并统一返回结果
	
	// 当然，管道的原理其实跟事务还是很不一样的。 
	//    事务是先把命令保存在一个队列中，等我们发送exec 命令以后，把整个队列数据发送给redis 服务端，让服务端按顺序执行。
	//    事务还可以配合watch 命令一起使用，实现乐观锁的效果。
	
	//    而管道说白了，其实就是根据redis 客户端与服务端之间的通信协议
	//    把多条命令拼接成一个很长的字符串（其实是流数据），然后redis 服务端拿到这个数据以后，能够按照协议去解析
	//    把流数据还原成多条命令，并按顺序执行
	
	// ===> 现在我们可以回答以前的疑问了，在命令行模式能使用管道吗？ 
	//      应该也可以，但是自己根据底层协议去拼接多条命令是比较累的。
	//      一般我们都使用封装好的工具类去操作，比如Jedis 的pipeLine
	@Test
	public void test() {
		// 获取管道对象
		Pipeline pipelined = jedis.pipelined();
		
		// 计算执行时间
		long start = System.currentTimeMillis();
		
		// 使用管道插入 10万条数据
		for (int i = 0; i < 100000; i++) {
			// 【注意】 这里的set 方法返回值是个 response 
			//        跟以前的事务操作一样，在管道命令没有统一发送之前，你虽然能拿到 response 对象
			//        但是调用这个方法的话，会直接报错
			pipelined.set(UUID.randomUUID().toString(), UUID.randomUUID().toString());
		}
		pipelined.sync();// 同步获取所有的回应
        
		// 计算使用管道插入数据所用时间
		System.out.println(System.currentTimeMillis() - start);
		
		// 重新获取 start 时间
		start = System.currentTimeMillis();
		// 使用原生  jedis 插入10万条数据， 命令会发送一条，执行一条
		for (int i = 0; i < 100000; i++) {
			jedis.set(UUID.randomUUID().toString(), UUID.randomUUID().toString());
		}
		
		// 计算不使用管道所耗费时间
		System.out.println(System.currentTimeMillis() - start);
	}
}
