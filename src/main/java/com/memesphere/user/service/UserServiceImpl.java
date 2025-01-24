package com.memesphere.user.service;

import com.memesphere.user.domain.User;
import com.memesphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public User findByLoginId(Long loginId) {
        return userRepository.findByLoginId(loginId).orElse(null);
    }

    public void save(User user){
        userRepository.save(user);
    }
}