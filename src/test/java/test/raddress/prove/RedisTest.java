package test.raddress.prove;

import org.junit.Assert;
import org.junit.Ignore;
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
        try (Jedis redis = new Jedis("localhost")) {
            String val = "figàta!";            
            redis.set("test:java: 2", val);
            log.info(redis.get("test:java: 2"));
            
            Assert.assertEquals(val, redis.get("test:java: 2"));
        }

    }

}
