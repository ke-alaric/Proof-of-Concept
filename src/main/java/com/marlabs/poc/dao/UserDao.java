package com.marlabs.poc.dao;

import org.springframework.stereotype.Repository;

import redis.clients.jedis.Jedis;

@Repository
public class UserDao {
	
    public boolean checkIfKeyExist(String key) {
        try (Jedis jedis = new Jedis("localhost",6379)) {
            return jedis.exists(key);
        }
    }
    
}
