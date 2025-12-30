package com.Shadows.authservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonDeserialize(using = RoleDeserializerInline.class)
    private Role role;

    // Inline deserializer class to handle role deserialization
    static class RoleDeserializerInline extends JsonDeserializer<Role> {
        @Override
        public Role deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getText();
            if (value == null || value.trim().isEmpty()) {
                return Role.CLIENT;
            }
            try {
                return Role.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to CLIENT if invalid role provided
                return Role.CLIENT;
            }
        }
    }
}