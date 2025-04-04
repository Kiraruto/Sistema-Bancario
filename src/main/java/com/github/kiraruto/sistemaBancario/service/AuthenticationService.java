package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.JWTAuthenticationresponse;
import com.github.kiraruto.sistemaBancario.dto.RefreshTokenRequest;
import com.github.kiraruto.sistemaBancario.dto.SigninRequest;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;


    public JWTAuthenticationresponse signin(SigninRequest singninRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(singninRequest.email(), singninRequest.password()));

        UserDetails user = userRepository.findByEmail(singninRequest.email())
                .orElseThrow(() -> new IllegalArgumentException("Email invalido ou senha."));
        var jwt = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);

        return new JWTAuthenticationresponse(jwt, refreshToken);
    }

    public JWTAuthenticationresponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String userEmail = jwtService.extractUserName(refreshTokenRequest.token());
        User user = userRepository.findByUsername(userEmail).orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com email: " + userEmail));

        if (jwtService.isTokenValid(refreshTokenRequest.token(), user)) {
            var jwt = jwtService.generateToken(user);

            return new JWTAuthenticationresponse(jwt, refreshTokenRequest.token());
        }

        return null;
    }
}
