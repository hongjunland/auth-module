package com.authmodule.user.application.service;

import com.authmodule.common.UseCase;
import com.authmodule.common.exception.ErrorMessage;
import com.authmodule.common.exception.UserBadCredentialsException;
import com.authmodule.common.util.security.Token;
import com.authmodule.common.util.security.UserDetailsImpl;
import com.authmodule.user.application.port.in.*;
import com.authmodule.user.application.port.out.LoginResponse;
import com.authmodule.user.application.port.out.PasswordEncoderPort;
import com.authmodule.user.application.port.out.ReadUserPort;
import com.authmodule.user.application.port.out.TokenGeneratorPort;
import com.authmodule.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@UseCase
@Transactional
public class LoginService implements LoginUseCase, UserDetailsService {
//    private final LoginPort loginPort;
    private final ReadUserPort readUserPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuthenticationManagerBuilder authBuilder;

    @Override
    public LoginResponse login(LoginCommand command) {
        User user = readUserPort.readByEmail(command.getEmail());

        Optional.of(passwordEncoderPort.matches(command.getPassword(), user.getPassword()))
                .filter(matches -> matches)
                .orElseThrow(() -> new UserBadCredentialsException(ErrorMessage.INVALID_PASSWORD.getMessage()));

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(command.getEmail(), command.getPassword());
        Authentication auth = authBuilder.getObject().authenticate(authToken);

        Token jwtToken = tokenGeneratorPort.generateToken(auth);

        return LoginResponse.builder()
                .accessToken(jwtToken.getAccessToken())
                .refreshToken(jwtToken.getRefreshToken())
                .expiration(jwtToken.getExpiration().toString())
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = readUserPort.readByEmail(username);
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return UserDetailsImpl.builder()
                .username(user.getId().toString())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
