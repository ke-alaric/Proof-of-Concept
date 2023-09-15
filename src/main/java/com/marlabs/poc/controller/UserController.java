package com.marlabs.poc.controller;

import static com.marlabs.poc.constants.UserConstants.*;

import java.util.Map;

import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        
        userService.saveUser(jsonObject, key);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(" {\"Message\": \"Successfully created user with key: " + key + "\"}");
    }
    
    @GetMapping(path = "/{objectType}/{objectId}", produces = "application/json")
    public ResponseEntity<Object> getUser(@RequestHeader HttpHeaders headers, @PathVariable String objectId,
                                                 @PathVariable String objectType) {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }

        String redisKey = objectType + PRE_ID_DELIMITER + objectId;
        if (!userService.existsRedisKey(redisKey)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject()
                            .put("Error", "ObjectId doesn't exist!")
                            .toString());
        }

        String receivedETag = headers.getFirst(IF_NONE_MATCH_HEADER);
        String actualEtag = "";
        if (objectType.equals(USER_OBJECT_TYPE)) {
            actualEtag = userService.getUserEtag(redisKey);
            if (receivedETag != null && receivedETag.equals(actualEtag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(actualEtag)
                        .body(" {\"Message\": \"Medical plan hasn't been modified!" + "\"}");
            }
        }

        Map<String, Object> plan = userService.getUser(redisKey);
        return ResponseEntity
                .ok()
                .eTag(actualEtag)
                .body(new JSONObject(plan).toString());
    }
    
    @DeleteMapping(path = "/{objectType}/{objectId}", produces = "application/json")
    public ResponseEntity<Object> deleteUser(@RequestHeader HttpHeaders headers, @PathVariable String objectId,
    												@PathVariable String objectType) {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }

        String redisKey = objectType + PRE_ID_DELIMITER + objectId;
        if (!userService.existsRedisKey(redisKey)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject()
                            .put("Warning", "ObjectId doesn't exist!")
                            .toString());
        }

        String receivedETag = headers.getFirst(IF_MATCH_HEADER);
        String actualEtag = userService.getUserEtag(redisKey);
        if (receivedETag != null && !receivedETag.equals(actualEtag)) {
            return ResponseEntity
                    .status(HttpStatus.PRECONDITION_FAILED)
                    .eTag(actualEtag)
                    .build();
        }

        userService.deleteUser(redisKey);

        return ResponseEntity
                .noContent()
                .build();
    }
    
    @PatchMapping(path = "/{objectType}/{objectId}", produces = "application/json")
    public ResponseEntity<Object> patchPlan(@RequestHeader HttpHeaders headers, @RequestBody String user,
                                            @PathVariable String objectId, @PathVariable String objectType) {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(user);
        } catch (JSONException jsonException) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject().put("Error", "Illegal Json data received!").toString());
        }

        String redisKey = objectType + PRE_ID_DELIMITER + objectId;
        if (!userService.existsRedisKey(redisKey)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("Message", "ObjectId does not exist").toString());
        }

        String receivedETag = headers.getFirst(IF_MATCH_HEADER);
        String actualEtag = userService.getUserEtag(redisKey);
        if (receivedETag != null && !receivedETag.equals(actualEtag)) {
            return ResponseEntity
                    .status(HttpStatus.PRECONDITION_FAILED)
                    .eTag(actualEtag)
                    .build();
        }

        String newEtag = userService.saveUser(jsonObject, redisKey);
        return ResponseEntity
                .ok()
                .eTag(newEtag)
                .body(new JSONObject().put("Message: ", "Successfully updated!").toString());
    }
    
}
