package com.kavindu.techmart.common.interfaces;

import com.kavindu.techmart.common.dto.UserDTO;

public interface AuthServiceLocal {

    UserDTO login(String username, String password);

    void logout(String token);

    UserDTO register(UserDTO user, String rawPassword);

    UserDTO validateToken(String token);

    UserDTO getUserById(Long id);

    void changePassword(Long userId, String oldPassword, String newPassword);

    long getActiveSessionCount();
}
