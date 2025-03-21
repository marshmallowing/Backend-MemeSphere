package com.memesphere.domain.user.service;

import com.memesphere.domain.user.converter.UserConverter;
import com.memesphere.domain.user.entity.User;
import com.memesphere.domain.user.dto.response.LoginResponse;
import com.memesphere.domain.user.dto.response.TokenResponse;
import com.memesphere.domain.user.dto.response.KakaoUserInfoResponse;
import com.memesphere.global.jwt.TokenProvider;
import com.memesphere.domain.user.repository.UserRepository;
import com.memesphere.global.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoServiceImpl implements KakaoService {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RedisService redisService;

    @Value("${security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.provider.kakao.token-uri}")
    private String tokenUri;

    @Value("${security.oauth2.client.provider.kakao.user-info-uri}")
    private String userInfoUri;

    public TokenResponse getAccessTokenFromKakao(String code) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String uri = UriComponentsBuilder.fromUriString(tokenUri)
                    .queryParam("grant_type", "authorization_code")
                    .queryParam("client_id", clientId)
                    .queryParam("code", code)
                    .toUriString();

            ResponseEntity<TokenResponse> responseEntity = restTemplate.postForEntity(uri, null, TokenResponse.class);
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("Error occurred while getting access token from Kakao: ", e);
            throw new RuntimeException("Failed to retrieve access token from Kakao", e);
        }
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String uri = UriComponentsBuilder.fromUriString(userInfoUri).toUriString();

            ResponseEntity<KakaoUserInfoResponse> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, entity, KakaoUserInfoResponse.class);
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("Error occurred while getting user info from Kakao: ", e);
            throw new RuntimeException("Failed to retrieve user info from Kakao", e);
        }
    }

    public LoginResponse handleUserLogin(KakaoUserInfoResponse kakaoUserInfoResponse) {
        User existingUser = userRepository.findByLoginId(kakaoUserInfoResponse.getId()).orElse(null);
        String accessToken;

        if (existingUser != null) {

            accessToken = tokenProvider.createAccessToken(existingUser.getEmail(), existingUser.getLoginId());
            String refreshToken = tokenProvider.createRefreshToken(existingUser.getEmail());

            String nickname = existingUser.getNickname();

            userRepository.save(existingUser);
            redisService.setValue(existingUser.getEmail(), refreshToken, 1000 * 60 * 60 * 24 * 7L);

            return new LoginResponse(accessToken, refreshToken, nickname);
        } else {
            User newUser = UserConverter.toKakaoUser(kakaoUserInfoResponse);
            newUser = userRepository.save(newUser);

            accessToken = tokenProvider.createAccessToken(newUser.getEmail(), newUser.getLoginId());
            String refreshToken = tokenProvider.createRefreshToken(newUser.getEmail());

            String nickname = newUser.getNickname();

            userRepository.save(newUser);

            return new LoginResponse(accessToken, refreshToken, nickname);
        }
    }
}
