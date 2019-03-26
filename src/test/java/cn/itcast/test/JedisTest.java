package cn.itcast.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

public class JedisTest {
	
	// 测试连接redis 数据库
	@Test
	public void test() {
		// 直接 new 一个 Jedis 对象
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		// 如果我们的 redis 有设置密码的话，那么我们可以使用这个方法来添加密码
		//jedis.auth("123456");
		
		// 首先，我们需要先使用 jedis.ping() 方法测试一下能不能成功连接 redis 服务器
		// 如果能成功连接，会返回一个  "pong" 字符串
		System.out.println(jedis.ping());
		
		// 用完以后，记得要close() 一下
		jedis.close();
	}
	
	// 使用jedis 给redis 发送命令
	// 先发送一些最简单的命令吧
	@Test
	public  void test2() {
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		// 往redis 数据库中添加几个字符串类型的数据
		jedis.set("stu:001", "张三");
		jedis.set("stu:002", "李四");
		jedis.set("stu:003", "王五");
		jedis.set("stu:004", "赵六");
		
		// 查看一下redis 数据库中现在有什么key 
		System.out.println(jedis.keys("*"));
		
		// 再然后，我们试图从数据库取一些字符串类型的数据
		System.out.println("stu:001===>" + jedis.get("stu:001"));
		System.out.println("msg===>" + jedis.get("msg"));
		jedis.close();
	}
	
	@Test
	public void test3() {
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		// mset 命令
		jedis.mset("teacher:001", "老张", 
				   "teacher:002", "老李",
				   "teacher:003", "老王",
				   "teacher:004", "老赵");
		
		// mget 命令
		List<String> list = jedis.mget("stu:001", "stu:002", "stu:003","stu:004");
		if(list != null && list.size()>0) {
			for (String string : list) {
				System.out.println(string);
			}
		}
		
		// exists 命令
		System.out.println(jedis.exists("stu:001"));
		
		// type 命令
		System.out.println(jedis.type("stu:001"));
		
		// expire 命令
		System.out.println(jedis.expire("stu:001", 3));
		jedis.close();
	}
	
	@Test
	public void test4() {
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		// 保存一个数字字符串，然后我们要来测试一下 incr 、 decr / incrBy / decrBy
		jedis.set("count", "1");
		
		// incr 命令
		System.out.println(jedis.incr("count"));
		
		// decr 命令
		System.out.println(jedis.decr("count"));
		
		// incrBy 命令
		System.out.println(jedis.incrBy("count", 10L));
		
		// decrBy 命令
		System.out.println(jedis.decrBy("count", 100L));
		
		// incrByFloat 命令
		System.out.println(jedis.incrByFloat("count", 100.0));
		
		// 没有 decrByFloat 命令，我们可以使用 incrByFloat 命令，加一个负数
		System.out.println(jedis.incrByFloat("count", -10.0));
		jedis.close();
	}
	
	@Test
	public void test5() {
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		// 字符串特有的命令
		jedis.set("str", "hello");
		
		jedis.append("str", " world");
		
		System.out.println(jedis.get("str"));
		System.out.println(jedis.strlen("str"));
		
		jedis.close();
	}
	
	// hash 类型数据的常用命令
	@Test
	public void test6() {
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		// hset 命令
		jedis.hset("emp:001", "empName", "eric");
		jedis.hset("emp:001", "age", "22");
		jedis.hset("emp:001", "gender", "male");
		
		// 我们也可以直接传一个 key + map 集合
		HashMap<String, String> map = new HashMap<>();
		map.put("empName", "rose");
		map.put("age", "25");
		map.put("gender", "female");
		
		jedis.hset("emp:002", map);
		
		// 我们直接一次性获取一个hash 类型的全部字段值，返回一个 map
		Map<String, String> map001 = jedis.hgetAll("emp:001");
		System.out.println(map001);
		
		// 获取一个字段的值
		System.out.println(jedis.hget("emp:002","empName"));
		
		// 获取多个字段的值
		List<String> hmget = jedis.hmget("emp:002", "empName", "age");
		System.out.println(hmget);
		
		// 跟 hgetAll() 命令很像，只不过返回的是一个 list 集合
		List<String> hvals = jedis.hvals("emp:002");
		System.out.println(hvals);
		jedis.close();
	}
	
	// 列表类型的常用命令
	@Test
	public void test7() {
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		// lPush() / Rpush() 
		// jedis.lpush("list", "001", "002", "003");
		// jedis.rpush("list2", "001", "002", "003");
		
		// 获取列表的全部值
		System.out.println(jedis.lrange("list", 0, -1));
		System.out.println(jedis.lrange("list2", 0, -1));
		
		// lpop / rpop
		System.out.println(jedis.lpop("list"));
		System.out.println(jedis.rpop("list2"));
		
		jedis.close();
	}
	
	@Test
	public void test8() {
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		// BLpop / BRpop 
		// 直接阻塞式地弹出一个不存在的键好了, 一定要设置延迟，时间是秒
		// 如果这个键不存在，那么等待10秒后， 返回一个 null
		//System.out.println(jedis.blpop(10, "list3"));
		
		// BLpop 还可以同时获取多个队列的数据，可以实现优先级队列
		System.out.println(jedis.blpop(10, "list3", "list4"));
		jedis.close();
	}
	
	// 演示一下集合的常用命令
	@Test
	public void test9() {
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		jedis.sadd("set", "rose" , "jack" , "tom", "jerry");
		Set<String> set = jedis.smembers("set");
		System.out.println(set);
		
		// 再添加一个集合，用来演示一下集合的基本操作
		jedis.sadd("set2", "rose" , "tina" , "eric", "jerry");
		
		// Sdiff 命令，获取一个集合特有的元素
		Set<String> sdiff1 = jedis.sdiff("set", "set2");
		System.out.println(sdiff1);
		
		Set<String> sdiff2 = jedis.sdiff("set2", "set");
		System.out.println(sdiff2);
		
		// Sunion 命令，获取两个集合的并集
		Set<String> sunion = jedis.sunion("set", "set2");
		System.out.println(sunion);
		
		// Sinter 命令，获取两个集合的交集
		Set<String> sinter = jedis.sinter("set", "set2");
		System.out.println(sinter);
		jedis.close();
	}
	
	// 演示一下有序集合的常用命令
	// 命令太多，不可能都演示，需要的时候，请拿着redis 的笔记出来看下
	// 只要知道命令，几乎就可以找到同名的方法
	@Test
	public void test10() {
		Jedis jedis = new Jedis("192.168.48.128", 6379);
		
		// 保存一个有序集合 score, 分数就是本次英文考试的分数
		jedis.zadd("english:score", 80.0, "eric");
		
		// 我们也可以直接保存一个 map 
		HashMap<String, Double> map = new HashMap<>();
		map.put("rose", 88.0);
		map.put("jack", 99.4);
		map.put("tom", 67.5);
		map.put("jerry", 75.7);
		jedis.zadd("english:score", map);
		System.out.println("********************");
		
		// 根据排名的索引 获取整个有序集合的全部数据
		// 不带分数
		Set<String> zrange = jedis.zrange("english:score", 0, -1);
		if(zrange != null && zrange.size() > 0) {
			for (String string : zrange) {
				System.out.println(string);
			}
		}
		System.out.println("********************");
		
		// 根据排名的索引获取整个有序集合的全部数据
		// 带分数 
		Set<Tuple> zrangeWithScores = jedis.zrangeWithScores("english:score", 0, -1);
		if(zrangeWithScores != null && zrangeWithScores.size() > 0) {
			for (Tuple tuple : zrangeWithScores) {
				System.out.println(tuple.getElement() + " : " + tuple.getScore());
			}
		}
		System.out.println("********************");
		
		// 根据排名的索引获取整个有序集合的全部数据
		// 带分数
		// 从大到小排序 
		Set<Tuple> zrevrangeWithScores = jedis.zrevrangeWithScores("english:score", 0, -1);
		if(zrevrangeWithScores != null && zrevrangeWithScores.size() > 0) {
			for (Tuple tuple : zrevrangeWithScores) {
				System.out.println(tuple.getElement() + " : " + tuple.getScore());
			}
		}
		System.out.println("********************");
		
		// 获取指定分数段中的全部数据
		Set<Tuple> zrangeByScoreWithScores = jedis.zrangeByScoreWithScores("english:score", 60.0, 80.0);
		if(zrangeByScoreWithScores != null && zrangeByScoreWithScores.size() > 0) {
			for (Tuple tuple : zrangeByScoreWithScores) {
				System.out.println(tuple.getElement() + " : " + tuple.getScore());
			}
		}
		System.out.println("********************");
		jedis.close();
	}
}
