package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.UserDto;
import com.screenleads.backend.app.web.dto.UserCreationResponse;

import java.util.List;

public interface UserService {
    List<UserDto> getAll();

    UserDto getById(Long id);

    UserCreationResponse create(UserDto dto);

    UserDto update(Long id, UserDto dto);

    void delete(Long id);
}
