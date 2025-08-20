package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.UserDto; // <-- usa tu UserDto existente

import java.util.List;

public interface UserService {
    List<UserDto> getAll();

    UserDto getById(Long id);

    void delete(Long id);

    // Si más adelante quieres CRUD completo:
    // UserDto create(UserDto dto);
    // UserDto update(Long id, UserDto dto);
}
