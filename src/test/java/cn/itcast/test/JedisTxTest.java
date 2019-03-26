package cn.itcast.test;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

public class JedisTxTest {
	private Jedis jedis;
	private Transaction transaction;
	
	@Before
	public void before() {
		jedis = new Jedis("192.168.48.128", 6379);
	}
	
	@After
	public void after() {
		
		if(jedis!=null) {
			jedis.close();
		}
		
		if(transaction != null) {
			transaction.close();
		}
	}
	
	@Test
	public void  test() {
		// redis 也是支持事务操作的，不过redis 的事务本身是一组命令的集合
		// 这些命令要么一起执行，要么一起不执行
		// redis 服务器只会使用一个线程来执行所有客户端的命令，所以本身有点像是mysql 中的串行化隔离级别
		// redis 不仅可以保证一个事务中的命令一起执行或者一起不执行，还可以保证这些命令执行过程中，不会被其他事务命令插入
		transaction = jedis.multi();
		// 拿到了事务对象以后，我们就可以使用事务进行增删改查，就像以前我们使用jedis 发送命令一样
		// 只是事务内的命令并不会马上执行，而是会被保存在一个 queue 队列里面，到时候会一起发送给redis 执行
		// 最后再返回结果
		transaction.set("tx", "1");
		transaction.incr("tx");
		transaction.incrBy("tx", 10L);
		
		// 最后我们要提交事务
		// exec() 方法，最终会返回批量执行的结果
		List<Object> exec = transaction.exec();
		if(exec != null && exec.size() > 0) {
			for (Object object : exec) {
				System.out.println(object);
			}
		}
		
		// 当然，我们也可以取消事务
		// transaction.discard();
		
	}
	
	// 这里我们一定要注意一个问题：
	// redis 的事务跟mysql 里面的事务完全不一样
	// redis 的事务中的命令并不会马上执行，而mysql 事务中的sql 语句都是可以独立执行的
	// 所以如果你的某些操作需要先从数据库获取值，然后再根据获取的值，决定下一步该怎么做，
	// 那么仅仅依靠redis 事务是无法实现的。
	// 甚至我们可以去看 transaction.get(key)   方法，这个方法的返回值可不是像以前的一个String
	// 而是一个 response 
	@Test
	public void test2() {
		transaction = jedis.multi();
		Response<String> response = transaction.get("tx");
		// 这里返回的是一个 reponse 对象，但是如果你在事务结束以前，直接调用这个response 对象的get 方法
		// 想要拿到返回值的话，那么Jedis 会给你报一个异常：
		// JedisDataException: Please close pipeline or multi block before calling this method.
		// 我们学过 redis 原理，应该能理解，response 对象里面的值必须要在命令执行完以后才会有。
		// 但是在事务提交之前，这些命令都还没有被执行，所以根本不会有结果。 因此，你应该可以推测，这个response
		// 对象有点像是那种懒加载，或者异步加载的对象
		System.out.println(response.get());
		transaction.exec();
	}
	
	
	// 如果我们就想先查一下 tx 的值，然后看值有没有大于100 ，
	//   如果大于100，那么我们就加10
	//   如果没有大于100，那么我们就加100
	
	// 像上面的这种需求，必须先连接 redis 获取值，然后再决定如何操作
	// 那么获取值的步骤，我们必须放在事务外面来处理
	// 得到具体的值以后，在事务中，根据前面的值来决定如何处理
	@Test
	public void test3() {
		// 在开启事务前，就需要先获取某个key 的值
		Integer result = null;
		
		try {
			// 先试图获取tx key 的值，并转成 Integer 类型
			// 如果值不存在 、或者这个值不是一个数字字符串，或者格式不对，都会抛异常
			// 这时候我们就先把 tx 的值转成一个默认的数字字符串
			result = Integer.parseInt(jedis.get("tx"));
		}catch(Exception e) {
			result = 0;
			jedis.set("tx", String.valueOf(result));
		}
		
		// 开启事务
		transaction = jedis.multi();
		if(result > 100) {
			transaction.set("tx", String.valueOf(result + 100));
		}else {
			transaction.set("tx", String.valueOf(result + 10));
		}
		
		// 最后执行事务
	    transaction.exec();
	}
	
	// 但是像上面的代码其实是有问题的：
	//  因为redis 的事务只保证事务内部的命令可以一起执行或者一起不执行
	//  事务外部的命令，比如说 jedis.get("tx")   和    jedis.set("tx", String.valueOf(result));
	//  这两条命令还有事务本身，redis 是无法保证是连续执行的
	
	//  于是就可能出现这种情况：   程序最开始执行  jedis.get("tx") ,拿到的值是 120 ，那么不会报异常，result = 120
	//  但是在开始执行事务命令之前，其他的客户端又修改了这个 tx 的值，为 10
	//  可是这个时候，程序并不知道服务器中的那个值已经改了，他还一直以为是之前获取的 120 
	//  于是就执行 transaction.set("tx", String.valueOf(result + 10));  操作
	
	// 像这种情况，在mysql 里面说的是丢失更新
	// 在java 多线程里面，说的是 线程安全问题
	//  而在redis 某些书上，是说这种情况就是产生了竞态条件（产生了竞态条件就有可能出现上面的那种问题）
	
	// 解决的思路一般有两种：  加锁（悲观锁）  和   更新时验证条件（乐观锁）
	// java 中解决线程安全问题，一般都是加锁，不管是synchronized 还是  lock 
	// mysql 中也支持加锁，  select xxx for update / select xxx in share mode 
	// 因为sql 更新语句还支持添加条件，所以我们也可以添加一个 version 列，在更新的时候去校验 version 值（乐观锁）
	// 那么redis 的解决方案是什么呢？
	//    加锁：  redis 对性能的追求近乎变态，所以redis 是不支持加锁的
	//    更新时验证条件：  大部分的 redis 命令都是不支持去校验某个key 的值是否等于xxx 的
	
	//  ===> 但是redis 提供了watch 命令。这个命令必须要配合事务才会有效果，相当于给事务是否执行添加验证条件。
	
	@Test
	public void test4() {
		while(true) {
			// 首先，我们应该先使用watch 命令，对某个key 或者 多个key 进行监视
			jedis.watch("tx");
			
			// 然后，在开启事务前，我们同样去获取这个key 的值
			Integer result = null;
			
			try {
				// 把key 的值转成 整数
				result = Integer.parseInt(jedis.get("tx"));
			}catch(Exception e) {
				result = 0;
				// 如果key 的值不存在 ，或者不合法，那么我们就给这个 key 设置一个初始值 
				// 【注意】 当我们在这里给 key 设置初始值以后，那么相当于 key 已经被改变了
		        //        所以后面的事务根本就不会执行
				jedis.set("tx", String.valueOf(result));
			}
			
			// 开启事务
			transaction = jedis.multi();
			if(result > 100) {
				transaction.set("tx", String.valueOf(result + 100));
			}else {
				transaction.set("tx", String.valueOf(result + 10));
			}
			
			// 最后执行事务
		    List<Object> exec = transaction.exec();
		    
		    // 我们可以根据   transaction.exec() 方法的返回值来判断事务有没有执行
		    //  如果返回值是 null ，那么说明因为监视的key 发生了变化，事务被取消
		    //       这里我们需要说一下，如果事务被监视器取消的话，那么我们可以直接给上一层报错
		    //       也可以再重新监视、获取值、判断值、执行事务===> 一般就是在外部套个死循环，成功以后才退出循环
		    //           【注意】 如果你套个死循环，一定要注意排除错误，别一直死循环！！
		    //  如果返回的不是 null ，那么说明事务有执行的，我们还可以根据不同的返回结果做更加细致的判断
		    if(exec != null) {
		    	// 修改成功
		    	System.out.println("修改成功");
		    	// 这里我们就不那么麻烦了，再去判断返回的结果是什么，直接break;
		    	break;
		    }else {
		    	// 修改失败
		    	System.out.println("修改失败，再来一次");
		    }
		    
		    // 虽然有  jedis.unWatch() 方法，可以取消监视，但是一般我们都是用不到这个命令的
		    // 所以这里也不演示了
		}
	}
	
}
