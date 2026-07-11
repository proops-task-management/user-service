package com.proops2026.userservice.util;

import com.proops2026.userservice.model.User;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    // Rotation-ready key identifier — the gateway selects the matching public key by this kid (ADR-005)
    private static final String KEY_ID = "proops-v1";

    @Value("${jwt.private-key}")
    private String privateKeyPem;

    @Value("${jwt.expires-in}")
    private long expiresIn;

    private PrivateKey privateKey;

    @PostConstruct
    void init() {
        this.privateKey = parsePrivateKey(privateKeyPem);
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiresIn);

        return Jwts.builder()
                .header().keyId(KEY_ID).and()
                .id(UUID.randomUUID().toString())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private PrivateKey parsePrivateKey(String pem) {
        // Strip any PEM banner (-----BEGIN/END … KEY-----) via regex, then keep only base64 chars.
        // Using a regex (not the literal banner string) also avoids a gitleaks private-key false-positive.
        String base64 = pem
                .replaceAll("-----[^-]+-----", "")
                .replaceAll("[^A-Za-z0-9+/=]", "");
        byte[] der = Base64.getDecoder().decode(base64);
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RS256 JWT private key", e);
        }
    }
}
