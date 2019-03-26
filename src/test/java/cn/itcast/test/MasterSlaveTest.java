package cn.itcast.test;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;

public class MasterSlaveTest {
	private Jedis masterJedis;
	private Jedis slaveJedis;
	
	@Before
	public void before() {
		masterJedis = new Jedis("192.168.48.128", 6379);
		slaveJedis = new Jedis("192.168.48.128", 6380);
	}
	
	@After
	public void after() {
		if(masterJedis != null) {
			masterJedis.close();
		}
		
		if(slaveJedis != null) {
			slaveJedis.close();
		}
	}
	
	// 关于主库从库的配置，我们之前已经讲过了，如果忘记的话，可以回过头再去看一下
	// 这里我们真使用已经配置好一个主库和从库
	
	// 这里演示的主要目的是：在主从复制、读写分离的情况下，是否可以保证数据的一致性的问题
	
	// 虽然我们的演示大多数时候是同步的，但是其实主库和从库的数据是无法保持绝对的同步的。
	// 这里面会存在着一个时间差的问题，我们以前也大概了解了了redis 主从复制的原理：
	//  就是从库会去读取主库的快照文件  xxx.rdb  , 然后把快照文件数据恢复到自己的数据库中
	//  以下的因素可能会影响主从库之间同步的时间差：
	//    1、 主从服务器之间的网络情况如果不通畅的话，那么很可能同步就比较慢了。（比如说主从服务器离得很远）
	//    2、 主服务器或者从服务器的运行状态也会影响同步。 如果主服务器非常繁忙，那么同步的速度肯定就慢了，反之也是一样的。
	//    3、 需要同步的数据量大小不同，也会影响到同步的效率。比如一台从服务器刚配置 slaveof  xxxx xx ，主库有20G数据要同步
	
	// 主从复制的延迟问题的例子：
	// 比如说： 小王发了一条微博，发了微博肯定是需要通过主库进行写操作。
	//       但是朋友们同时去刷的话，可能并不会马上就看到小王发的这条微博，因为朋友们查看微博，只是读操作
	//       读取的操作往往是从从库中读取的，而此时朋友分配到的那台服务器上可能还没有同步到小王发的那条微博数据
	//       一般过个几十秒或者一分钟，应该就可以看到了
	@Test
	public void test() {
		String timeValue = String.valueOf( new Date().getTime());
		// 一般情况下，我们都是在主库上面写入数据
		masterJedis.set("mk", timeValue);
		// 然后使用从库读取数据
		String result = slaveJedis.get("mk");
		System.out.println("result: " + result);
		System.out.println("timeValue: " + timeValue);
	}
}
