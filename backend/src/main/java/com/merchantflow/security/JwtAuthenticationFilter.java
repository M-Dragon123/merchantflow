package com.merchantflow.security;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService; public JwtAuthenticationFilter(JwtService jwtService) { this.jwtService = jwtService; }
  @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
    String header = req.getHeader("Authorization"); if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(req, res); return; }
    try { Claims claims = jwtService.parse(header.substring(7)); List<String> roles = claims.get("roles", List.class); var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList()); SecurityContextHolder.getContext().setAuthentication(auth); } catch (RuntimeException ignored) { SecurityContextHolder.clearContext(); }
    chain.doFilter(req, res);
  }
}
