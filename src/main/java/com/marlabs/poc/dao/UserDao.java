package com.marlabs.poc.dao;

import static com.marlabs.poc.constants.UserConstants.PORT;
import static com.marlabs.poc.constants.UserConstants.REDIS_URL;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Repository;

import redis.clients.jedis.JedisPooled;

@Repository
public class UserDao {
	private final JedisPooled jedis = new JedisPooled(REDIS_URL, PORT);
	
    public void sadd(String key, String value) {
        jedis.sadd(key, value);
    }

    public Set<String> sMembers(String key) {
        return jedis.smembers(key);
    }

    public void hSet(String key, String field, String value) {
        jedis.hset(key, field, value);
    }

    public String hGet(String key, String field) {
        return jedis.hget(key, field);
    }

    public Map<String, String> hGetAll(String key) {
        return jedis.hgetAll(key);
    }

    public void lpush(String key, String value) {
        jedis.lpush(key, value);
    }

    public boolean existsKey(String key) {
        return jedis.exists(key);
    }

    public Set<String> getKeysByPattern(String pattern) {
        return jedis.keys(pattern);
    }

    public long deleteKeys(String[] keys) {
        return jedis.del(keys);
    }
}
