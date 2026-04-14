package com.proops2026.userservice.mapper;

import com.proops2026.userservice.dto.response.LoginResponse;
import com.proops2026.userservice.dto.response.UserResponse;
import com.proops2026.userservice.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    LoginResponse.UserInfo toUserInfo(User user);
}
