package com.marlabs.poc.controller;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class UserController {
	
    private RSAKey rsaPublicJWK;
    
    @GetMapping(value = "/getToken")
    public ResponseEntity<String> getToken()
            throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException,JOSEException, ParseException {

    		// RSA signatures require a public and private RSA key pair, the public key 
    		// must be made known to the JWS recipient in order to verify the signatures
    		RSAKey rsaJWK = new RSAKeyGenerator(2048).keyID("123").generate();
    		System.out.println(rsaJWK);
    		rsaPublicJWK = rsaJWK.toPublicJWK();
    		// verifier = new RSASSAVerifier(rsaPublicJWK);
    		System.out.println(rsaPublicJWK);
    		// Create RSA-signer with the private key
    		JWSSigner signer = new RSASSASigner(rsaJWK);

    		// Prepare JWT with claims set
    		int expireTime = 30000; // seconds
    		
    		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().expirationTime(new Date(new Date().getTime() + expireTime * 1000)) // milliseconds
    		    .build();

    		SignedJWT signedJWT = new SignedJWT(
    		    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
    		    claimsSet);

    		// Compute the RSA signature
    		signedJWT.sign(signer);
    		
    		String token = signedJWT.serialize();
    		
    		return ResponseEntity.status(HttpStatus.OK)
            .body(new JSONObject().put("Token:  ", token).toString());
    }
}
