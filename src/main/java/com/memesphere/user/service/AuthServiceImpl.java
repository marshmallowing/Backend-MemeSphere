package com.memesphere.user.service;

import com.memesphere.global.apipayload.code.status.ErrorStatus;
import com.memesphere.global.apipayload.exception.GeneralException;
import com.memesphere.global.jwt.TokenProvider;
import com.memesphere.user.converter.UserConverter;
import com.memesphere.user.domain.User;
import com.memesphere.user.dto.request.SignInRequest;
import com.memesphere.user.dto.request.SignUpRequest;
import com.memesphere.user.dto.response.LoginResponse;
import com.memesphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserServiceImpl userServiceImpl;
    private final TokenProvider tokenProvider;

    public void handleUserRegistration(SignUpRequest signUpRequest) {
        User existingUser = userRepository.findByEmail(signUpRequest.getEmail()).orElse(null);

        if (existingUser != null) {
            throw new GeneralException(ErrorStatus.USER_ALREADY_EXISTS);
        }

        if (checkNicknameDuplicate(signUpRequest.getNickname())) {
            throw new GeneralException(ErrorStatus.NICKNAME_ALREADY_EXISTS);
        }

        User newUser = UserConverter.toAuthUser(signUpRequest, passwordEncoder);
        userServiceImpl.save(newUser);
    }

    public LoginResponse handleUserLogin(SignInRequest signInRequest) {
        User existingUser = userRepository.findByEmail(signInRequest.getEmail()).orElse(null);
        String accessToken;

        if (existingUser != null) {

            checkPassword(existingUser, signInRequest.getPassword());

            accessToken = tokenProvider.createAccessToken(existingUser.getEmail(), existingUser.getLoginId());
            String refreshToken = tokenProvider.createRefreshToken(existingUser.getEmail());

            existingUser.setAccessToken(accessToken);
            existingUser.setRefreshToken(refreshToken);
            userRepository.save(existingUser);
            return new LoginResponse(accessToken, refreshToken);
        } else {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }
    }

    public void checkPassword(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new GeneralException(ErrorStatus.PASSWORD_NOT_MATCH);
        }
    }

    public boolean checkNicknameDuplicate(String nickname) {
        return userRepository.findByNickname(nickname).isPresent();
    }
}
