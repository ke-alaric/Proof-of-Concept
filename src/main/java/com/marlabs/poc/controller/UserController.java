package com.marlabs.poc.controller;

import static com.marlabs.poc.constants.UserConstants.OBJECT_ID_MAME;
import static com.marlabs.poc.constants.UserConstants.OBJECT_TYPE_NAME;
import static com.marlabs.poc.constants.UserConstants.PRE_ID_DELIMITER;

import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marlabs.poc.service.UserService;
import com.marlabs.poc.service.AuthorizationService;

@RestController
@RequestMapping(path = "/poc")
public class UserController {
	
    @Autowired
    UserService userService;
    
    
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
    
    @PostMapping(path = "/user", produces = "application/json")
    public ResponseEntity<Object> createMedicalPlan(@RequestBody String plan, @RequestHeader HttpHeaders headers)
            throws Exception {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }

        if (plan == null || plan.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject().put("Error", "Empty user received!").toString());
        }

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(plan);
        } catch (JSONException jsonException) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject().put("Error", "Illegal Json data received!").toString());
        }


        String key = jsonObject.get(OBJECT_TYPE_NAME).toString()
                + PRE_ID_DELIMITER
                + jsonObject.get(OBJECT_ID_MAME).toString();
        if (userService.existsRedisKey(key)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new JSONObject()
                            .put("Warning", "User already exists!")
                            .toString());
        }
        
        userService.saveMedicalPlan(jsonObject, key);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(" {\"Message\": \"Successfully created user with key: " + key + "\"}");
    }
    
}
