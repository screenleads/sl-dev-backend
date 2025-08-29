package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.UserDto;
import com.screenleads.backend.app.web.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final CompanyRepository companyRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public UserServiceImpl(
            UserRepository repo,
            CompanyRepository companyRepo,
            RoleRepository roleRepo,
            PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.companyRepo = companyRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserDto> getAll() {
        return repo.findAll().stream()
                .map(UserMapper::toDto)
                .toList();
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

    @Override
    public UserDto create(UserDto dto) {
        if (dto == null)
            throw new IllegalArgumentException("Body requerido");
        if (dto.getUsername() == null || dto.getUsername().isBlank())
            throw new IllegalArgumentException("username requerido");

        repo.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("username ya existe");
        });

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setName(dto.getName());
        u.setLastName(dto.getLastName());

        // <<< CAMBIO CLAVE >>>
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().isBlank())
                ? dto.getPassword()
                : generateTempPassword(12);
        u.setPassword(passwordEncoder.encode(rawPassword));
        // <<< FIN CAMBIO >>>

        if (dto.getCompanyId() != null) {
            Company c = companyRepo.findById(dto.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + dto.getCompanyId()));
            u.setCompany(c);
        }

        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String rn : dto.getRoles()) {
                Role r = roleRepo.findByRole(rn)
                        .orElseThrow(() -> new IllegalArgumentException("role inválido: " + rn));
                roles.add(r);
            }
            u.setRoles(roles);
        } else {
            u.setRoles(Set.of());
        }

        User saved = repo.save(u);

        // Opcional: si generaste una contraseña temporal, devuélvela aparte
        // (mejor por email o solo una vez en la respuesta)
        UserDto res = UserMapper.toDto(saved);
        // res.setTempPassword(dto.getPassword() == null ? rawPassword : null); // si tu
        // DTO lo soporta
        return res;
    }

    @Override
    public UserDto update(Long id, UserDto dto) {
        return repo.findById(id).map(existing -> {
            if (dto.getUsername() != null)
                existing.setUsername(dto.getUsername());
            if (dto.getEmail() != null)
                existing.setEmail(dto.getEmail());
            if (dto.getName() != null)
                existing.setName(dto.getName());
            if (dto.getLastName() != null)
                existing.setLastName(dto.getLastName());

            // <<< PERMITIR CAMBIO DE PASSWORD >>>
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            // <<< FIN >>>

            if (dto.getCompanyId() != null) {
                Company c = companyRepo.findById(dto.getCompanyId())
                        .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + dto.getCompanyId()));
                existing.setCompany(c);
            }

            if (dto.getRoles() != null) {
                Set<Role> roles = new HashSet<>();
                for (String rn : dto.getRoles()) {
                    Role r = roleRepo.findByRole(rn)
                            .orElseThrow(() -> new IllegalArgumentException("role inválido: " + rn));
                    roles.add(r);
                }
                existing.setRoles(roles);
            }

            User saved = repo.save(existing);
            return UserMapper.toDto(saved);
        }).orElse(null);
    }

    private String generateTempPassword(int length) {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return sb.toString();
    }
}
