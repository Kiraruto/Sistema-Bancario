package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.dto.JWTAuthenticationresponse;
import com.github.kiraruto.sistemaBancario.dto.RefreshTokenRequest;
import com.github.kiraruto.sistemaBancario.dto.SigninRequest;
import com.github.kiraruto.sistemaBancario.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/signin")
    public ResponseEntity<JWTAuthenticationresponse> signin(@RequestBody @Valid SigninRequest signinRequest) {
        return ResponseEntity.ok(authenticationService.signin(signinRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JWTAuthenticationresponse> refresh(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authenticationService.refreshToken(refreshTokenRequest));
    }
}
