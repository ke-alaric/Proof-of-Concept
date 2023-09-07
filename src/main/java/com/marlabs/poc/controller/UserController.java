package com.marlabs.poc.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marlabs.poc.service.AuthorizationService;

@RestController
@RequestMapping(path = "/poc")
public class UserController {
	
    @Autowired
    private AuthorizationService authorizationService;
	
    @GetMapping(value = "/token")
    public ResponseEntity<String> getToken() {
        String token = authorizationService.getToken();
        return new ResponseEntity<>(token, HttpStatus.CREATED);
    }

    @PostMapping(value = "/token")
    public ResponseEntity<String> verifyToken(@RequestHeader HttpHeaders headers) {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(" {\"Message\": \"Token verified!" + "\"}");
    }
    
}
