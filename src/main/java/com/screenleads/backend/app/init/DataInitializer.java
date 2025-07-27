package com.screenleads.backend.app.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final MediaTypeRepository mediaTypeRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final CompanyRepository companyRepository;

    @Override
    public void run(String... args) {
        createDefaultCompany("ScreenLeads", "Compañía por defecto para demo");
        createRole("ROLE_ADMIN", "Acceso total", 1);
        createRole("ROLE_COMPANY_ADMIN", "Administrador de empresa", 2);
        createRole("ROLE_COMPANY_MANAGER", "Gestor de empresa", 3);
        createRole("ROLE_COMPANY_VIEWER", "Visualizador de empresa", 4);
        createMediaTypes("video/mp4", "mp4");
        createMediaTypes("video/webm", "webm");
        createMediaTypes("video/avi", "avi");
        createMediaTypes("video/mpeg", "mpeg");
        createMediaTypes("video/quicktime", "mov");
        createMediaTypes("video/x-msvideo", "avi");
        createMediaTypes("video/x-flv", "flv");
        createMediaTypes("image/jpeg", "jpg");
        createMediaTypes("image/png", "png");
        createMediaTypes("image/gif", "gif");
        createMediaTypes("image/webp", "webp");
        createDeviceTypes("tv");
        createDeviceTypes("mobile");
        createDeviceTypes("desktop");
        createDeviceTypes("tablet");
        createDeviceTypes("other");

    }

    private void createRole(String role, String desc, int level) {
        if (!roleRepository.existsByRole(role)) {
            roleRepository.save(Role.builder()
                    .role(role)
                    .description(desc)
                    .level(level)
                    .build());
        }
    }

    private void createMediaTypes(String type, String extension) {
        if (!mediaTypeRepository.existsByType(type)) {
            mediaTypeRepository.save(MediaType.builder()
                    .type(type)
                    .extension(extension)
                    .build());
        }
    }

    private void createDeviceTypes(String type) {
        if (!deviceTypeRepository.existsByType(type)) {
            deviceTypeRepository.save(DeviceType.builder()
                    .type(type)
                    .build());
        }
    }

    private void createDefaultCompany(String name, String observations) {
        if (!companyRepository.existsByName(name)) {
            companyRepository.save(Company.builder()
                    .name(name)
                    .observations(observations)
                    .build());
        }
    }
}