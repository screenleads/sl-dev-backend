package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.UserDto;
import com.screenleads.backend.app.web.mapper.UserMapper; // <-- usa tu mapper existente
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repo;

    public UserServiceImpl(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<UserDto> getAll() {
        return repo.findAll().stream()
                .map(UserMapper::toDto) // referencia estática a método
                .toList(); // o Collectors.toList()
    }

    @Override
    public UserDto getById(Long id) {
        return repo.findById(id)
                .map(UserMapper::toDto)
                .orElse(null);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
