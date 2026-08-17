package com.merchantflow.security;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
class JwtServiceTests { @Test void createsAndParsesToken() { JwtService service = new JwtService("a-32-character-minimum-development-secret-for-tests"); assertEquals("admin", service.parse(service.createToken(1L, "admin", List.of("ADMIN"))).getSubject()); } }
