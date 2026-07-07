package com.emailservice.application.port;

import com.emailservice.domain.entity.User;

/**
 * Port for issuing authentication tokens, implemented by the infrastructure layer.
 */
public interface TokenService {
    String generateToken(User user);
}
