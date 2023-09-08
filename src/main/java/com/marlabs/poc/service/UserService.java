package com.marlabs.poc.service;

import com.marlabs.poc.dao.UserDao;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.marlabs.poc.constants.UserConstants.*;

@Service
public class UserService {
	
    @Autowired
    UserDao userDao;

    public String saveMedicalPlan(JSONObject planObject, String key) {
        Map<String, Object> saveMedicalPlanMap = saveMedicalPlanToRedis(key, planObject);
        String saveMedicalPlanString = new JSONObject(saveMedicalPlanMap).toString();

        String newEtag = DigestUtils.md5DigestAsHex(saveMedicalPlanString.getBytes(StandardCharsets.UTF_8));
        userDao.hSet(key, ETAG_KEY_NAME, newEtag);
        return newEtag;
    }

    public Map<String, Object> saveMedicalPlanToRedis(String key, JSONObject planObject) {
        saveJSONObjectToRedis(planObject);
        return getMedicalPlan(key);
    }

    public Map<String, Object> getMedicalPlan(String redisKey) {
        Map<String, Object> outputMap = new HashMap<>();

        Set<String> keys = userDao.getKeysByPattern(redisKey + REDIS_ALL_PATTERN);
        for (String key : keys) {
            if (key.equals(redisKey)) {
                Map<String, String> value = userDao.hGetAll(key);
                for (String name : value.keySet()) {
                    if (!name.equalsIgnoreCase(ETAG_KEY_NAME)) {
                        outputMap.put(name,
                                isDouble(value.get(name)) ? Double.parseDouble(value.get(name)) : value.get(name));
                    }
                }
            } else {
                String newKey = key.substring((redisKey + PRE_FIELD_DELIMITER).length());
                Set<String> members = userDao.sMembers(key);
                if (members.size() > 1) {
                    List<Object> listObj = new ArrayList<>();
                    for (String member : members) {
                        listObj.add(getMedicalPlan(member));
                    }
                    outputMap.put(newKey, listObj);
                } else {
                    Map<String, String> val = userDao.hGetAll(members.iterator().next());
                    Map<String, Object> newMap = new HashMap<>();
                    for (String name : val.keySet()) {
                        newMap.put(name,
                                isDouble(val.get(name)) ? Double.parseDouble(val.get(name)) : val.get(name));
                    }
                    outputMap.put(newKey, newMap);
                }
            }
        }

        return outputMap;
    }

    public void deleteMedicalPlan(String redisKey) {
        Set<String> keys = userDao.getKeysByPattern(redisKey + REDIS_ALL_PATTERN);
        for (String key : keys) { 
            if (key.equals(redisKey)) {
                userDao.deleteKeys(new String[]{key});
            } else {
                Set<String> members = userDao.sMembers(key);
                if (members.size() > 1) {
                    for (String member : members) {
                        deleteMedicalPlan(member);
                    }
                    userDao.deleteKeys(new String[]{key});
                } else {
                    userDao.deleteKeys(new String[]{members.iterator().next(), key});
                }
            }
        }
    }

    public boolean existsRedisKey(String key) {
        return userDao.existsKey(key);
    }

    public String getMedicalPlanEtag(String key) {
        return userDao.hGet(key, ETAG_KEY_NAME);
    }

    private Map<String, Map<String, Object>> saveJSONObjectToRedis(JSONObject object) {
        Map<String, Map<String, Object>> redisKeyMap = new HashMap<>();
        Map<String, Object> objectFieldMap = new HashMap<>();

        String redisKey = object.get(OBJECT_TYPE_NAME) + PRE_ID_DELIMITER + object.get(OBJECT_ID_MAME);
        for (String field : object.keySet()) {
            Object value = object.get(field);
            if (value instanceof JSONObject) {
                Map<String, Map<String, Object>> convertedValue = saveJSONObjectToRedis((JSONObject) value);
                userDao.sadd(redisKey + PRE_FIELD_DELIMITER + field,
                        convertedValue.entrySet().iterator().next().getKey());
            } else if (value instanceof JSONArray) {
                List<Map<String, Map<String, Object>>> convertedValue = saveJSONArrayToRedis((JSONArray) value);
                for (Map<String, Map<String, Object>> entry : convertedValue) {
                    for (String listKey : entry.keySet()) {
                        userDao.sadd(redisKey + PRE_FIELD_DELIMITER + field, listKey);
                    }
                }
            } else {
                userDao.hSet(redisKey, field, value.toString());
                objectFieldMap.put(field, value);
                redisKeyMap.put(redisKey, objectFieldMap);
            }
        }

        return redisKeyMap;
    }

    private List<Map<String, Map<String, Object>>> saveJSONArrayToRedis(JSONArray array) {
        List<Map<String, Map<String, Object>>> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                List<Map<String, Map<String, Object>>> convertedValue = saveJSONArrayToRedis((JSONArray) value);
                list.addAll(convertedValue);
            } else if (value instanceof JSONObject) {
                Map<String, Map<String, Object>> convertedValue = saveJSONObjectToRedis((JSONObject) value);
                list.add(convertedValue);
            }
        }
        return list;
    }

    private boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException numberFormatException) {
            return false;
        }
    }
}
