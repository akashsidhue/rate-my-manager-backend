package com.ratemymanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;

public class JwtUtil {
	private static final String JWT_SECRET_BASE64 = "bXktdmVyeS1sb25nLXN1cGVyLXNlY3JldC1rZXktZm9yLWp3dC10ZXN0aW5nLW9ubHk=";

	private static Key getSigningKey() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET_BASE64));
	}

	public static String extractSubject(String token) {
		Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
		return claims.getSubject();
	}
} 