package com.merchantflow.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service public class JwtService {
  private final SecretKey key;
  public JwtService(@Value("${merchantflow.security.jwt-secret}") String secret) { key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }
  public String createToken(Long id, String username, Collection<String> roles) { Instant now = Instant.now(); return Jwts.builder().subject(username).claim("uid", id).claim("roles", roles).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(28800))).signWith(key).compact(); }
  public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
