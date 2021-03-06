package net.jadedungeon.javautil.mongo;

import static net.jadedungeon.javautil.mongo.Condition.newCondition;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;

import net.jadedungeon.javautil.mongo.Condition.LinkType;
import net.jadedungeon.javautil.mongo.Condition.Option;
import net.jadedungeon.javautil.mongo.impl.MongoUtil;

;

public class MongoUtilTest {

	private User user = null;

	@Before
	public void setUp() throws Exception {
		Map<String, String> ss = new HashMap<>();
		ss.put("key1", "value1");
		ss.put("key2", "value2");
		ss.put("key3", "value3");
		user = new User("A001", "Jade", 3, Arrays.asList(new UserAuth("func1",
				true), new UserAuth("func2", false)), Arrays.asList("aaa",
				"bbb"), ss, true);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRecTable() {
		assertEquals("user", MongoUtil.getCollectionNameFromOjbect(user));
	}

	@Test
	public void testGenRec() throws IllegalArgumentException,
			IllegalAccessException {
		assertEquals(
				"{ \"id\" : \"A001\" , \"name\" : \"Jade\" , \"level\" : 3 , "
						+ "\"authList\" : [ { \"func\" : \"func1\" , \"authed\" : true} , "
						+ "{ \"func\" : \"func2\" , \"authed\" : false}] , \"listExample\" : "
						+ "[ \"aaa\" , \"bbb\"] , \"mapExample\" : { \"key1\" : \"value1\" , "
						+ "\"key2\" : \"value2\" , \"key3\" : \"value3\"} , \"onDuby\" : true}",
				MongoUtil.genRecFromModel(user).toString());
	}

	@Test
	public void testGenObj() throws InstantiationException,
			IllegalAccessException {
		Object obj = MongoUtil.genModelFromRec(User.class,
				MongoUtil.genRecFromModel(user));
		assertEquals(user, obj);
	}

	@Test
	public void testCondition00() throws IllegalArgumentException,
			IllegalAccessException {
		// {j: 3, k: 10}
		BasicDBObject q1 = new BasicDBObject("j", 3).append("k", 10).append("l", 12);
		Condition c = newCondition("j", 3).append(LinkType.AND,
				newCondition("k", 10)).append(LinkType.AND, newCondition("l", 12));
		BasicDBObject q2 = MongoUtil.parseCondition(c);
		assertEquals(q1, q2);
	}

	@Test
	public void testCondition01() throws IllegalArgumentException,
			IllegalAccessException {
		// {j: {$ne: 3}, k: {$gt: 10} }
		BasicDBObject q1 = new BasicDBObject("j", new BasicDBObject("$ne", 3))
				.append("k", new BasicDBObject("$gt", 10));
		Condition c = newCondition("j", newCondition(Option.NE, 3));
		c.append(LinkType.AND, newCondition("k", newCondition(Option.GT, 10)));
		BasicDBObject q2 = MongoUtil.parseCondition(c);
		assertEquals(q1, q2);
	}

	@Test
	public void testCondition02() throws IllegalArgumentException,
			IllegalAccessException {
		// 20 < i <= 30
		// { "i" : { "$gt" : 20 , "$lte" : 30}}
		BasicDBObject q1 = new BasicDBObject("i",
				new BasicDBObject("$gt", 20).append("$lte", 30));
		Condition c = newCondition("i", newCondition(Option.GT, 20).append(//
				LinkType.AND, newCondition(Option.LTE, 30)));
		BasicDBObject q2 = MongoUtil.parseCondition(c);
		assertEquals(q1, q2);
	}

	@Test
	public void testCondition03() throws IllegalArgumentException,
			IllegalAccessException {
		// { "$set" : { "auths" : [ { "func" : "func1" , "authed" : true} ,
		//   { "func" : "func2" , "authed" : false}]}}
		List<BasicDBObject> bl = new ArrayList<BasicDBObject>();
		bl.add(new BasicDBObject("func", "func1").append("authed", true));
		bl.add(new BasicDBObject("func", "func2").append("authed", false));
		BasicDBObject q1 = new BasicDBObject("$set",
				new BasicDBObject("authList", bl));
		List<UserAuth> l = new ArrayList<UserAuth>();
		l.add(new UserAuth("func1", true));
		l.add(new UserAuth("func2", false));
		Condition c = newCondition("$set", newCondition("authList", l));
		BasicDBObject q2 = MongoUtil.parseCondition(c);
//		System.out.println(q1);
//		System.out.println(q2);
		assertEquals(q1, q2);
	}

	@Test
	public void test() {
		// MongoUtil.addAttribute(null, 3);
		String[] aa = { "aaa", "bbb", "ccc" };
		assertEquals(true, aa instanceof Object[]);
	}

}
