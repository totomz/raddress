package test.raddress.prove;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

public class RedisTest {

	static {
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug");
		System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_DATE_TIME_KEY, "true");
		System.setProperty(org.slf4j.impl.SimpleLogger.DATE_TIME_FORMAT_KEY, "HH:mm:ss.SSS");
	}
	
	private static Logger log = LoggerFactory.getLogger(RedisTest.class);
	
	@Test
	public void TestConneciton() {
		log.info("Bella zi!");
		
		try(Jedis redis = new Jedis("104.155.40.33")) {
			//redis.set("test:java: 2", "figàta!");
			//log.info("Fatto?");
			
			log.info(redis.get("test:java: 2"));
		}
		
		
	}
	
}
