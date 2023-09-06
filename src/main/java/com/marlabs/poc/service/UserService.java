package com.marlabs.poc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.marlabs.poc.dao.UserDao;


@Service
public class UserService {
	
    @Autowired
    UserDao userDao;
    
}
