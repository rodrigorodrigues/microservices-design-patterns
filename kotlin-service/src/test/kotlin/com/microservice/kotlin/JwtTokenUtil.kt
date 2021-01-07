package com.microservice.kotlin

import com.microservice.authentication.autoconfigure.AuthenticationProperties
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import net.minidev.json.JSONObject
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import java.time.ZonedDateTime
import java.util.*

class JwtTokenUtil(private val authenticationProperties: AuthenticationProperties) {
    fun createToken(usernamePasswordAuthenticationToken: UsernamePasswordAuthenticationToken): String {
        val jwtClaimsSet = JWTClaimsSet.Builder()
            .subject(usernamePasswordAuthenticationToken.name)
            .expirationTime(Date.from(ZonedDateTime.now().plusMinutes(1).toInstant()))
            .issueTime(Date())
            .notBeforeTime(Date())
            .claim("authorities", usernamePasswordAuthenticationToken.authorities.map { it.authority }.toList())
            .jwtID(UUID.randomUUID().toString())
            .issuer("jwt")
            .build()
        val signer: JWSSigner = MACSigner(authenticationProperties.jwt.keyValue)
        val jsonObject = JSONObject()
        jsonObject["kid"] = "test"
        jsonObject["alg"] = JWSAlgorithm.HS256.name
        jsonObject["typ"] = "JWT"
        val signedJWT = SignedJWT(JWSHeader.parse(jsonObject), jwtClaimsSet)
        signedJWT.sign(signer)
        return "Bearer " + signedJWT.serialize()
    }
}
