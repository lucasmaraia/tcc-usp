package com.emailservice.application.port;

import com.emailservice.domain.entity.User;

public interface TokenService {
    String generateToken(User user);
}
