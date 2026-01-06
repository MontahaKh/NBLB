package com.Shadows.recommendationservice.controller;

import com.Shadows.recommendationservice.model.ProductDto;
import com.Shadows.recommendationservice.service.GeminiService;
import com.Shadows.recommendationservice.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final GeminiService geminiService;
    private final JwtUtil jwtUtil;

    public RecommendationController(GeminiService geminiService, JwtUtil jwtUtil) {
        this.geminiService = geminiService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> getRecommendations(@RequestHeader(value = "Authorization", required = false) String authorization) {
        // Security Check
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Please log in.");
        }

        return ResponseEntity.ok(geminiService.getRecommendations());
    }
}
