package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.web.dto.RoleDTO;
import com.screenleads.backend.app.web.mapper.RoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository repo;

    public RoleServiceImpl(RoleRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<RoleDTO> getAll() {
        return repo.findAll().stream().map(RoleMapper::toDTO).toList();
    }

    @Override
    public RoleDTO getById(Long id) {
        return repo.findById(id).map(RoleMapper::toDTO).orElse(null);
    }

    @Override
    public RoleDTO create(RoleDTO dto) {
        Role toSave = RoleMapper.toEntity(dto);
        Role saved = repo.save(toSave);
        return RoleMapper.toDTO(saved);
    }

    @Override
    public RoleDTO update(Long id, RoleDTO dto) {
        Role existing = repo.findById(id).orElseThrow();
        existing.setRole(dto.role());
        existing.setDescription(dto.description());
        existing.setLevel(dto.level());
        return RoleMapper.toDTO(repo.save(existing));
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
