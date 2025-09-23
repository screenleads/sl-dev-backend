# Servicios — snapshot incrustado

> Capa de servicios.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/application/service/AdviceService.java
package com.screenleads.backend.app.application.service;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.AdviceDTO;

public interface AdviceService {
    List<AdviceDTO> getAllAdvices();

    /** Devuelve los advices visibles "ahora" en la zoneId indicada (si null, systemDefault). */
    List<AdviceDTO> getVisibleAdvicesNow(ZoneId zoneId);

    Optional<AdviceDTO> getAdviceById(Long id);

    AdviceDTO saveAdvice(AdviceDTO dto);

    AdviceDTO updateAdvice(Long id, AdviceDTO dto);

    void deleteAdvice(Long id);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/AdviceServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.*;

@Service
public class AdviceServiceImpl implements AdviceService {

    private final AdviceRepository adviceRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AdviceServiceImpl(AdviceRepository adviceRepository,
                             MediaRepository mediaRepository,
                             UserRepository userRepository) {
        this.adviceRepository = adviceRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
    }

    // ======================= LECTURAS =======================

    @Override
    @Transactional
    public List<AdviceDTO> getAllAdvices() {
        enableCompanyFilterIfNeeded();
        return adviceRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(AdviceDTO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AdviceDTO> getVisibleAdvicesNow(ZoneId zoneId) {
        enableCompanyFilterIfNeeded();

        ZoneId zone = (zoneId != null) ? zoneId : ZoneId.systemDefault();
        ZonedDateTime nowZ = ZonedDateTime.now(zone);
        LocalDate date = nowZ.toLocalDate();
        DayOfWeek weekday = nowZ.getDayOfWeek();
        LocalTime time = nowZ.toLocalTime();

        return adviceRepository.findAll().stream()
                .filter(a -> a.getSchedules() != null && !a.getSchedules().isEmpty())
                .filter(a -> isVisibleNow(a, date, weekday, time))
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(AdviceDTO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private boolean isVisibleNow(Advice a, LocalDate date, DayOfWeek weekday, LocalTime time) {
        for (AdviceSchedule s : a.getSchedules()) {
            boolean dateOk = (s.getStartDate() == null || !date.isBefore(s.getStartDate()))
                          && (s.getEndDate() == null   || !date.isAfter(s.getEndDate()));
            if (!dateOk) continue;

            // Si el día no tiene horas, no es visible ese día
            if (s.getWindows() == null || s.getWindows().isEmpty()) continue;

            boolean any = s.getWindows().stream().anyMatch(w ->
                    w.getWeekday() == weekday
                 && w.getFromTime() != null && w.getToTime() != null
                 && !time.isBefore(w.getFromTime())   // >= from
                 && time.isBefore(w.getToTime())       // < to (fin exclusivo)
            );
            if (any) return true;
        }
        return false;
    }

    @Override
    @Transactional
    public Optional<AdviceDTO> getAdviceById(Long id) {
        enableCompanyFilterIfNeeded();
        return adviceRepository.findById(id).map(this::convertToDTO);
    }

    // ============================= ESCRITURAS =============================

    @Override
    @Transactional
    public AdviceDTO saveAdvice(AdviceDTO dto) {
        enableCompanyFilterIfNeeded();

        Advice advice = new Advice();
        advice.setDescription(dto.getDescription());
        advice.setCustomInterval(Boolean.TRUE.equals(dto.getCustomInterval()));
        advice.setInterval(numberToDuration(dto.getInterval()));

        advice.setCompany(resolveCompanyForWrite(dto.getCompany(), null));
        advice.setMedia(resolveMediaFromDto(dto.getMedia()));
        advice.setPromotion(resolvePromotionFromDto(dto.getPromotion()));

        // schedules
        advice.setSchedules(new ArrayList<>());
        if (dto.getSchedules() != null) {
            for (AdviceScheduleDTO sDto : dto.getSchedules()) {
                advice.getSchedules().add(mapScheduleDTO(sDto, advice));
            }
        }

        validateAdvice(advice);
        Advice saved = adviceRepository.save(advice);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public AdviceDTO updateAdvice(Long id, AdviceDTO dto) {
        enableCompanyFilterIfNeeded();

        Advice advice = adviceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Advice not found: " + id));

        advice.setDescription(dto.getDescription());
        advice.setCustomInterval(Boolean.TRUE.equals(dto.getCustomInterval()));
        advice.setInterval(numberToDuration(dto.getInterval()));

        advice.setCompany(resolveCompanyForWrite(dto.getCompany(), advice.getCompany()));
        advice.setMedia(resolveMediaFromDto(dto.getMedia()));
        advice.setPromotion(resolvePromotionFromDto(dto.getPromotion()));

        advice.getSchedules().clear();
        if (dto.getSchedules() != null) {
            for (AdviceScheduleDTO sDto : dto.getSchedules()) {
                advice.getSchedules().add(mapScheduleDTO(sDto, advice));
            }
        }

        validateAdvice(advice);
        Advice saved = adviceRepository.save(advice);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteAdvice(Long id) {
        enableCompanyFilterIfNeeded();
        adviceRepository.findById(id).ifPresent(adviceRepository::delete);
    }

    // ============================= VALIDACIONES =============================

    private void validateAdvice(Advice advice) {
        if (advice.getSchedules() == null) return;
        for (AdviceSchedule s : advice.getSchedules()) {
            if (s.getStartDate() != null && s.getEndDate() != null &&
                s.getStartDate().isAfter(s.getEndDate())) {
                throw new IllegalArgumentException("Schedule startDate must be <= endDate");
            }
            if (s.getWindows() != null) validateAndNormalizeWindows(s.getWindows());
        }
    }

    /** from<to, orden por día+hora, no solapes dentro del mismo día. */
    private void validateAndNormalizeWindows(List<AdviceTimeWindow> windows) {
        for (AdviceTimeWindow w : windows) {
            if (w.getWeekday() == null) throw new IllegalArgumentException("Window weekday is required");
            if (w.getFromTime() == null || w.getToTime() == null)
                throw new IllegalArgumentException("Window from/to must be set");
            if (!w.getFromTime().isBefore(w.getToTime()))
                throw new IllegalArgumentException("Window 'from' must be strictly before 'to'");
        }
        windows.sort(Comparator
                .comparing(AdviceTimeWindow::getWeekday)
                .thenComparing(AdviceTimeWindow::getFromTime)
                .thenComparing(AdviceTimeWindow::getToTime));

        for (int i = 1; i < windows.size(); i++) {
            AdviceTimeWindow prev = windows.get(i - 1);
            AdviceTimeWindow cur  = windows.get(i);
            if (cur.getWeekday() == prev.getWeekday()
             && cur.getFromTime().isBefore(prev.getToTime())) {
                throw new IllegalArgumentException(
                        "Overlapping windows on " + prev.getWeekday()
                        + ": [" + prev.getFromTime() + "-" + prev.getToTime() + "] with ["
                        + cur.getFromTime() + "-" + cur.getToTime() + "]");
            }
        }
    }

    // ============================= MAPEOS =============================

    private AdviceSchedule mapScheduleDTO(AdviceScheduleDTO sDto, Advice owner) {
        AdviceSchedule s = new AdviceSchedule();
        s.setAdvice(owner);
        s.setStartDate(parseDate(sDto.getStartDate()));
        s.setEndDate(parseDate(sDto.getEndDate()));

        List<AdviceTimeWindow> windows = new ArrayList<>();
        if (sDto.getWindows() != null) {
            for (AdviceTimeWindowDTO wDto : sDto.getWindows()) {
                AdviceTimeWindow w = new AdviceTimeWindow();
                w.setSchedule(s);
                w.setWeekday(parseWeekday(wDto.getWeekday()));
                w.setFromTime(parseTime(wDto.getFromTime()));
                w.setToTime(parseTime(wDto.getToTime()));
                windows.add(w);
            }
        }
        validateAndNormalizeWindows(windows);
        s.setWindows(windows);
        return s;
    }

    private AdviceDTO convertToDTO(Advice advice) {
        MediaUpsertDTO mediaDto = (advice.getMedia() != null)
                ? new MediaUpsertDTO(advice.getMedia().getId(), advice.getMedia().getSrc())
                : null;

        PromotionRefDTO promoDto = (advice.getPromotion() != null)
                ? new PromotionRefDTO(advice.getPromotion().getId())
                : null;

        CompanyRefDTO companyDto = (advice.getCompany() != null)
                ? new CompanyRefDTO(advice.getCompany().getId(), advice.getCompany().getName())
                : null;

        Number intervalValue = (advice.getInterval() == null) ? null : advice.getInterval().getSeconds();

        List<AdviceScheduleDTO> schedules = new ArrayList<>();
        if (advice.getSchedules() != null) {
            for (AdviceSchedule s : advice.getSchedules()) {
                List<AdviceTimeWindowDTO> wins = new ArrayList<>();
                if (s.getWindows() != null) {
                    for (AdviceTimeWindow w : s.getWindows()) {
                        wins.add(AdviceTimeWindowDTO.builder()
                                .id(w.getId())
                                .weekday(w.getWeekday() != null ? w.getWeekday().name() : null)
                                .fromTime(formatTime(w.getFromTime()))
                                .toTime(formatTime(w.getToTime()))
                                .build());
                    }
                }
                schedules.add(AdviceScheduleDTO.builder()
                        .id(s.getId())
                        .startDate(formatDate(s.getStartDate()))
                        .endDate(formatDate(s.getEndDate()))
                        .windows(wins)
                        .build());
            }
        }

        return AdviceDTO.builder()
                .id(advice.getId())
                .description(advice.getDescription())
                .customInterval(advice.getCustomInterval())
                .interval(intervalValue)
                .media(mediaDto)
                .promotion(promoDto)
                .company(companyDto)
                .schedules(schedules)
                .build();
    }

    // ============================= HELPERS =============================

    private Duration numberToDuration(Number n) {
        if (n == null) return null;
        long s = n.longValue();
        return (s > 0) ? Duration.ofSeconds(s) : null;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.trim());
    }
    private String formatDate(LocalDate d) {
        return (d == null) ? null : d.toString();
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        if (t.matches("^\\d{2}:\\d{2}$")) t = t + ":00";
        return LocalTime.parse(t, DateTimeFormatter.ISO_LOCAL_TIME);
    }
    private String formatTime(LocalTime t) {
        return (t == null) ? null : t.toString(); // HH:mm:ss
    }

    private DayOfWeek parseWeekday(String s) {
        if (s == null || s.isBlank()) return null;
        return DayOfWeek.valueOf(s.trim().toUpperCase());
    }

    private Media resolveMediaFromDto(MediaUpsertDTO incoming) {
        if (incoming == null) return null;

        if (incoming.id() != null && incoming.id() > 0) {
            return mediaRepository.findById(incoming.id())
                    .orElseThrow(() -> new IllegalArgumentException("Media no encontrada (id=" + incoming.id() + ")"));
        }
        String src = incoming.src() != null ? incoming.src().trim() : null;
        if (src != null && !src.isEmpty()) {
            Media existing = mediaRepository.findBySrc(src).orElse(null);
            if (existing != null) return existing;
            Media m = new Media();
            m.setSrc(src);
            return mediaRepository.save(m);
        }
        return null;
    }

    private Promotion resolvePromotionFromDto(PromotionRefDTO incoming) {
        if (incoming == null) return null;
        Long id = incoming.id();
        if (id == null || id <= 0) return null;
        return entityManager.getReference(Promotion.class, id);
    }

    private Company resolveCompanyForWrite(CompanyRefDTO incoming, Company current) {
        boolean isAdmin = isCurrentUserAdmin();
        Long userCompanyId = currentCompanyId();

        if (!isAdmin) {
            if (userCompanyId != null) {
                Company c = new Company();
                c.setId(userCompanyId);
                return c;
            }
            return current;
        }

        Long desiredId = (incoming != null) ? incoming.id() : null;
        if (desiredId != null && desiredId > 0) {
            Company c = new Company();
            c.setId(desiredId);
            return c;
        }
        if (current != null) return current;
        if (userCompanyId != null) {
            Company c = new Company();
            c.setId(userCompanyId);
            return c;
        }
        return null;
    }

    /** Activa el filtro "companyFilter" si NO es admin. */
    private void enableCompanyFilterIfNeeded() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return;

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
        if (isAdmin) return;

        Long companyId = resolveCompanyId(auth);
        if (companyId == null) return;

        Session session = entityManager.unwrap(Session.class);
        var filter = session.getEnabledFilter("companyFilter");
        if (filter == null) {
            session.enableFilter("companyFilter").setParameter("companyId", companyId);
        } else {
            filter.setParameter("companyId", companyId);
        }
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
    }

    private Long currentCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return resolveCompanyId(auth);
    }

    private Long resolveCompanyId(Authentication auth) {
        Object principal = auth.getPrincipal();

        if (principal instanceof com.screenleads.backend.app.domain.model.User u) {
            return (u.getCompany() != null) ? u.getCompany().getId() : null;
        }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }
        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }
        return null;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/AppVersionService.java
package com.screenleads.backend.app.application.service;

import java.util.List;

import com.screenleads.backend.app.web.dto.AppVersionDTO;

public interface AppVersionService {
    AppVersionDTO save(AppVersionDTO dto);

    List<AppVersionDTO> findAll();

    AppVersionDTO findById(Long id);

    void deleteById(Long id);

    AppVersionDTO getLatestVersion(String platform);
}
```

```java
// src/main/java/com/screenleads/backend/app/application/service/AppVersionServiceImpl.java
package com.screenleads.backend.app.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.AppVersion;
import com.screenleads.backend.app.domain.repositories.AppVersionRepository;
import com.screenleads.backend.app.web.dto.AppVersionDTO;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppVersionServiceImpl implements AppVersionService {

    private final AppVersionRepository repository;

    @Override
    public AppVersionDTO save(AppVersionDTO dto) {
        AppVersion entity = toEntity(dto);
        AppVersion saved = repository.save(entity);
        return toDTO(saved);
    }

    @Override
    public List<AppVersionDTO> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppVersionDTO findById(Long id) {
        AppVersion entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AppVersion not found with id " + id));
        return toDTO(entity);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public AppVersionDTO getLatestVersion(String platform) {
        AppVersion entity = repository.findTopByPlatformOrderByIdDesc(platform)
                .orElseThrow(() -> new RuntimeException("No version found for platform " + platform));
        return toDTO(entity);
    }

    // --- Métodos de conversión ---
    private AppVersionDTO toDTO(AppVersion entity) {
        return AppVersionDTO.builder()
                .id(entity.getId())
                .platform(entity.getPlatform())
                .version(entity.getVersion())
                .message(entity.getMessage())
                .url(entity.getUrl())
                .forceUpdate(entity.isForceUpdate())
                .build();
    }

    private AppVersion toEntity(AppVersionDTO dto) {
        return AppVersion.builder()
                .id(dto.getId())
                .platform(dto.getPlatform())
                .version(dto.getVersion())
                .message(dto.getMessage())
                .url(dto.getUrl())
                .forceUpdate(dto.isForceUpdate())
                .build();
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/CompaniesService.java
package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.CompanyDTO;

public interface CompaniesService {
    List<CompanyDTO> getAllCompanies();

    Optional<CompanyDTO> getCompanyById(Long id);

    CompanyDTO saveCompany(CompanyDTO CompanyDTO);

    CompanyDTO updateCompany(Long id, CompanyDTO CompanyDTO);

    void deleteCompany(Long id);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/CompaniesServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.repositories.*;
import com.screenleads.backend.app.web.dto.CompanyDTO;

@Service
public class CompaniesServiceImpl implements CompaniesService {

    private final MediaTypeRepository mediaTypeRepository;

    private final CompanyRepository companyRepository;
    private final MediaRepository mediaRepository;
    private final AdviceRepository adviceRepository;
    private final DeviceRepository deviceRepository;

    public CompaniesServiceImpl(CompanyRepository companyRepository, MediaRepository mediaRepository,
            AdviceRepository adviceRepository, DeviceRepository deviceRepository,
            MediaTypeRepository mediaTypeRepository) {
        this.companyRepository = companyRepository;
        this.mediaRepository = mediaRepository;
        this.adviceRepository = adviceRepository;
        this.deviceRepository = deviceRepository;
        this.mediaTypeRepository = mediaTypeRepository;
    }

    @Override
    public List<CompanyDTO> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(CompanyDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CompanyDTO> getCompanyById(Long id) {
        return companyRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public CompanyDTO saveCompany(CompanyDTO companyDTO) {
        Company company = convertToEntity(companyDTO);

        // Si ya existe por nombre, devolver el existente
        Optional<Company> exist = companyRepository.findByName(company.getName());
        if (exist.isPresent()) {
            return convertToDTO(exist.get());
        }

        // --- Manejo del logo ---
        if (companyDTO.logo() != null) {
            if (companyDTO.logo().getId() != null) {
                // Buscar el Media existente por id
                Media media = mediaRepository.findById(companyDTO.logo().getId())
                        .orElseThrow(
                                () -> new RuntimeException("Media no encontrado con id: " + companyDTO.logo().getId()));
                company.setLogo(media);

            } else if (companyDTO.logo().getSrc() != null && !companyDTO.logo().getSrc().isBlank()) {
                // Crear un nuevo Media con el src
                Media newLogo = new Media();
                newLogo.setSrc(companyDTO.logo().getSrc());

                // Detectar extensión
                String srcLower = companyDTO.logo().getSrc().toLowerCase();
                String extension = null;
                int dotIndex = srcLower.lastIndexOf('.');
                if (dotIndex != -1 && dotIndex < srcLower.length() - 1) {
                    extension = srcLower.substring(dotIndex + 1);
                }

                // Asignar MediaType según extensión
                if (extension != null) {
                    mediaTypeRepository.findByExtension(extension).ifPresent(newLogo::setType);
                }

                Media savedLogo = mediaRepository.save(newLogo);
                company.setLogo(savedLogo);

            } else {
                company.setLogo(null);
            }
        } else {
            company.setLogo(null);
        }

        Company savedCompany = companyRepository.save(company);
        return convertToDTO(savedCompany);
    }

    @Override
    public CompanyDTO updateCompany(Long id, CompanyDTO companyDTO) {
        Company company = companyRepository.findById(id).orElseThrow();
        company.setName(companyDTO.name());
        company.setObservations(companyDTO.observations());
        company.setPrimaryColor(companyDTO.primaryColor());
        company.setSecondaryColor(companyDTO.secondaryColor());

        // Manejo seguro del logo
        if (companyDTO.logo() != null) {
            if (companyDTO.logo().getId() != null) {
                Media media = mediaRepository.findById(companyDTO.logo().getId()).orElseThrow();

                String newSrc = companyDTO.logo().getSrc();
                if (newSrc != null && !newSrc.isBlank() && !java.util.Objects.equals(media.getSrc(), newSrc)) {
                    media.setSrc(newSrc);
                    mediaRepository.save(media);
                }

                company.setLogo(media);

            } else if (companyDTO.logo().getSrc() != null && !companyDTO.logo().getSrc().isBlank()) {
                // Crear nuevo Media desde cero
                Media newLogo = new Media();
                newLogo.setSrc(companyDTO.logo().getSrc());
                String srcLower = companyDTO.logo().getSrc().toLowerCase();
                String extension = null;
                int dotIndex = srcLower.lastIndexOf('.');
                if (dotIndex != -1 && dotIndex < srcLower.length() - 1) {
                    extension = srcLower.substring(dotIndex + 1);
                }

                // Asignar MediaType según extensión
                if (extension != null) {
                    mediaTypeRepository.findByExtension(extension).ifPresent(newLogo::setType);
                }
                Media savedLogo = mediaRepository.save(newLogo);
                company.setLogo(savedLogo);

            } else {
                company.setLogo(null);
            }
        } else {
            company.setLogo(null);
        }

        Company updatedCompany = companyRepository.save(company);
        return convertToDTO(updatedCompany);
    }

    @Override
    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }

    private CompanyDTO convertToDTO(Company company) {
        return new CompanyDTO(
                company.getId(),
                company.getName(),
                company.getObservations(),
                company.getLogo(),
                company.getDevices(),
                company.getAdvices(),
                company.getPrimaryColor(),
                company.getSecondaryColor());
    }

    private Company convertToEntity(CompanyDTO companyDTO) {
        Company company = new Company();
        company.setId(companyDTO.id());
        company.setName(companyDTO.name());
        company.setPrimaryColor(companyDTO.primaryColor());
        company.setSecondaryColor(companyDTO.secondaryColor());
        company.setObservations(companyDTO.observations());

        // Manejo seguro del logo
        if (companyDTO.logo() != null && companyDTO.logo().getId() != null) {
            mediaRepository.findById(companyDTO.logo().getId()).ifPresent(company::setLogo);
        } else {
            company.setLogo(null);
        }

        if (companyDTO.advices() != null) {
            List<Advice> advices = companyDTO.advices().stream()
                    .map(adviceDTO -> adviceRepository.findById(adviceDTO.getId()).orElse(null))
                    .filter(a -> a != null)
                    .peek(a -> a.setCompany(company))
                    .collect(Collectors.toList());
            company.setAdvices(advices);
        } else {
            company.setAdvices(List.of());
        }

        if (companyDTO.devices() != null) {
            List<Device> devices = companyDTO.devices().stream()
                    .map(deviceDTO -> deviceRepository.findById(deviceDTO.getId()).orElse(null))
                    .filter(d -> d != null)
                    .peek(d -> d.setCompany(company))
                    .collect(Collectors.toList());
            company.setDevices(devices);
        } else {
            company.setDevices(List.of());
        }

        return company;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/CouponService.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.PromotionLead;

public interface CouponService {

    // Emite un cupón (crea un lead histórico) para un customer dado
    PromotionLead issueCoupon(Long promotionId, Long customerId);

    // Validación por código (verifica fechas/estado y devuelve el lead)
    PromotionLead validate(String couponCode);

    // Canje (marca REDEEMED si es válido), devuelve lead actualizado
    PromotionLead redeem(String couponCode);

    // Caducar manualmente (o programáticamente)
    PromotionLead expire(String couponCode);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/CouponServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.application.service.util.CouponCodeGenerator;
import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;
import com.screenleads.backend.app.domain.repositories.PromotionLeadRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final PromotionRepository promotionRepository;
    private final PromotionLeadRepository promotionLeadRepository;
    private final CustomerRepository customerRepository;

    @Override
    public PromotionLead issueCoupon(Long promotionId, Long customerId) {
        Promotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        // Chequeo de ventana temporal de la promo
        Instant now = Instant.now();
        if (promotion.getStartAt() != null && now.isBefore(promotion.getStartAt())) {
            throw new IllegalStateException("Promotion not started yet");
        }
        if (promotion.getEndAt() != null && now.isAfter(promotion.getEndAt())) {
            throw new IllegalStateException("Promotion already ended");
        }

        // Enforce límites
        LeadLimitType limit = promotion.getLeadLimitType();
        if (limit == LeadLimitType.ONE_PER_PERSON) {
            long count = promotionLeadRepository.countByPromotionIdAndCustomerId(promotionId, customerId);
            if (count > 0) {
                throw new IllegalStateException("Limit reached: ONE_PER_PERSON");
            }
        } else if (limit == LeadLimitType.ONE_PER_24H) {
            Instant since = now.minus(24, ChronoUnit.HOURS);
            long count = promotionLeadRepository.countByPromotionAndCustomerSince(promotionId, customerId, since);
            if (count > 0) {
                throw new IllegalStateException("Limit reached: ONE_PER_24H");
            }
        }

        // Generar código único
        String code;
        do {
            code = CouponCodeGenerator.generate(12);
        } while (promotionLeadRepository.findByCouponCode(code).isPresent());

        // Crear lead (histórico)
        PromotionLead lead = PromotionLead.builder()
                .promotion(promotion)
                .customer(customer)
                .identifierType(promotion.getLeadIdentifierType())
                .identifier(customer.getIdentifier())
                .couponCode(code)
                .couponStatus(CouponStatus.VALID) // lo dejamos ya VALID si la promo está activa
                // opcional: establecer expiresAt = endAt de la promo
                .expiresAt(promotion.getEndAt())
                .build();

        return promotionLeadRepository.save(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionLead validate(String couponCode) {
        PromotionLead lead = promotionLeadRepository.findByCouponCode(couponCode)
            .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        Instant now = Instant.now();
        Promotion p = lead.getPromotion();

        if (lead.getCouponStatus() == CouponStatus.CANCELLED) {
            throw new IllegalStateException("Coupon cancelled");
        }
        if (lead.getCouponStatus() == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Coupon already redeemed");
        }
        if (lead.getCouponStatus() == CouponStatus.EXPIRED) {
            throw new IllegalStateException("Coupon expired");
        }
        if (lead.getExpiresAt() != null && now.isAfter(lead.getExpiresAt())) {
            throw new IllegalStateException("Coupon expired");
        }
        if (p.getStartAt() != null && now.isBefore(p.getStartAt())) {
            throw new IllegalStateException("Promotion not started yet");
        }
        if (p.getEndAt() != null && now.isAfter(p.getEndAt())) {
            throw new IllegalStateException("Promotion already ended");
        }
        return lead; // válido
    }

    @Override
    public PromotionLead redeem(String couponCode) {
        PromotionLead lead = promotionLeadRepository.findByCouponCode(couponCode)
            .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        // Validaciones reutilizando validate() (pero con escritura)
        Promotion p = lead.getPromotion();
        Instant now = Instant.now();

        if (lead.getCouponStatus() == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Coupon already redeemed");
        }
        if (lead.getCouponStatus() == CouponStatus.CANCELLED) {
            throw new IllegalStateException("Coupon cancelled");
        }
        if (lead.getCouponStatus() == CouponStatus.EXPIRED) {
            throw new IllegalStateException("Coupon expired");
        }
        if (lead.getExpiresAt() != null && now.isAfter(lead.getExpiresAt())) {
            lead.setCouponStatus(CouponStatus.EXPIRED);
            promotionLeadRepository.save(lead);
            throw new IllegalStateException("Coupon expired");
        }
        if (p.getStartAt() != null && now.isBefore(p.getStartAt())) {
            throw new IllegalStateException("Promotion not started yet");
        }
        if (p.getEndAt() != null && now.isAfter(p.getEndAt())) {
            lead.setCouponStatus(CouponStatus.EXPIRED);
            promotionLeadRepository.save(lead);
            throw new IllegalStateException("Promotion already ended");
        }

        lead.setCouponStatus(CouponStatus.REDEEMED);
        lead.setRedeemedAt(now);
        return promotionLeadRepository.save(lead);
    }

    @Override
    public PromotionLead expire(String couponCode) {
        PromotionLead lead = promotionLeadRepository.findByCouponCode(couponCode)
            .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        if (lead.getCouponStatus() == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Cannot expire a redeemed coupon");
        }

        lead.setCouponStatus(CouponStatus.EXPIRED);
        if (lead.getExpiresAt() == null) {
            lead.setExpiresAt(Instant.now());
        }
        return promotionLeadRepository.save(lead);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/CustomerService.java
package com.screenleads.backend.app.application.service;

import java.util.List;

import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;

public interface CustomerService {

    Customer create(Long companyId,
                    LeadIdentifierType identifierType,
                    String identifier,
                    String firstName,
                    String lastName);

    Customer update(Long id,
                    LeadIdentifierType identifierType,
                    String identifier,
                    String firstName,
                    String lastName);

    Customer get(Long id);

    List<Customer> list(Long companyId, String search);

    void delete(Long id);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/CustomerServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.application.service.util.IdentifierNormalizer;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;

    @Override
    public Customer create(Long companyId,
                           LeadIdentifierType identifierType,
                           String identifier,
                           String firstName,
                           String lastName) {

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        String normalized = IdentifierNormalizer.normalize(identifierType, identifier);

        // Enforce unicidad (company, type, identifier)
        customerRepository.findByCompanyIdAndIdentifierTypeAndIdentifier(companyId, identifierType, normalized)
            .ifPresent(c -> { throw new IllegalStateException("Customer already exists for this identifier"); });

        Customer c = Customer.builder()
                .company(company)
                .identifierType(identifierType)
                .identifier(normalized)
                .firstName(firstName)
                .lastName(lastName)
                .build();

        return customerRepository.save(c);
    }

    @Override
    public Customer update(Long id,
                           LeadIdentifierType identifierType,
                           String identifier,
                           String firstName,
                           String lastName) {

        Customer existing = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        String normalized = IdentifierNormalizer.normalize(identifierType, identifier);

        // Si cambia la clave única, comprobar colisión
        boolean keyChanged = existing.getIdentifierType() != identifierType
                || !existing.getIdentifier().equals(normalized);

        if (keyChanged) {
            customerRepository.findByCompanyIdAndIdentifierTypeAndIdentifier(
                    existing.getCompany().getId(), identifierType, normalized)
                .ifPresent(other -> {
                    if (!other.getId().equals(existing.getId())) {
                        throw new IllegalStateException("Another customer already uses this identifier");
                    }
                });
        }

        existing.setIdentifierType(identifierType);
        existing.setIdentifier(normalized);
        existing.setFirstName(firstName);
        existing.setLastName(lastName);

        return customerRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> list(Long companyId, String search) {
        if (companyId == null) {
            // Por simplicidad devolvemos todo; si prefieres obligar companyId, lanza excepción
            return customerRepository.findAll();
        }
        if (search == null || search.isBlank()) {
            return customerRepository.findByCompanyId(companyId);
        }
        return customerRepository.findByCompanyIdAndIdentifierContainingIgnoreCase(companyId, search.trim());
    }

    @Override
    public void delete(Long id) {
        Customer existing = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        // Si quieres proteger borrado cuando tiene leads, compruébalo aquí:
        // if (existing.getLeads() != null && !existing.getLeads().isEmpty()) { ... }

        customerRepository.delete(existing);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/DeviceService.java
package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;

public interface DeviceService {
    List<DeviceDTO> getAllDevices();

    Optional<DeviceDTO> getDeviceById(Long id);

    Optional<DeviceDTO> getDeviceByUuid(String uuid); // NUEVO

    DeviceDTO saveDevice(DeviceDTO deviceDTO);

    DeviceDTO updateDevice(Long id, DeviceDTO deviceDTO);

    void deleteDevice(Long id);

    void deleteByUuid(String uuid); // opcional

    List<AdviceDTO> getAdvicesForDevice(Long deviceId);

    void assignAdviceToDevice(Long deviceId, Long adviceId);

    void removeAdviceFromDevice(Long deviceId, Long adviceId);
}
```

```java
// src/main/java/com/screenleads/backend/app/application/service/DeviceServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;
import com.screenleads.backend.app.web.mapper.AdviceMapper;

@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final CompanyRepository companyRepository;
    private final AdviceRepository adviceRepository;

    public DeviceServiceImpl(
            DeviceRepository deviceRepository,
            DeviceTypeRepository deviceTypeRepository,
            CompanyRepository companyRepository,
            AdviceRepository adviceRepository) {
        this.deviceRepository = deviceRepository;
        this.deviceTypeRepository = deviceTypeRepository;
        this.companyRepository = companyRepository;
        this.adviceRepository = adviceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(DeviceDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceDTO> getDeviceById(Long id) {
        return deviceRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceDTO> getDeviceByUuid(String uuid) {
        return deviceRepository.findOptionalByUuid(uuid).map(this::convertToDTO);
    }

    @Override
    public DeviceDTO saveDevice(DeviceDTO dto) {
        // Upsert idempotente por UUID
        DeviceType type = deviceTypeRepository.findById(dto.type().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device type not found"));

        Device device = deviceRepository.findOptionalByUuid(dto.uuid()).orElseGet(Device::new);
        Integer width  = device.getWidth()  != null ? device.getWidth().intValue()  : null;
        Integer height = device.getHeight() != null ? device.getHeight().intValue() : null;

        device.setUuid(dto.uuid());
        device.setDescriptionName(dto.descriptionName());
        device.setWidth(width);
        device.setHeight(height);
        device.setType(type);

        if (dto.company() != null && dto.company().getId() != null) {
            Company company = companyRepository.findById(dto.company().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
            device.setCompany(company);
        } else {
            device.setCompany(null);
        }

        return convertToDTO(deviceRepository.save(device));
    }

    @Override
    public DeviceDTO updateDevice(Long id, DeviceDTO deviceDTO) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        if (deviceDTO.type() == null || deviceDTO.type().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device type is required");
        }

        DeviceType type = deviceTypeRepository.findById(deviceDTO.type().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device type not found"));

        device.setUuid(deviceDTO.uuid());
        device.setDescriptionName(deviceDTO.descriptionName());
        Integer width  = device.getWidth()  != null ? device.getWidth().intValue()  : null;
        Integer height = device.getHeight() != null ? device.getHeight().intValue() : null;
        device.setWidth(width);
        device.setHeight(height);
        device.setType(type);

        if (deviceDTO.company() != null && deviceDTO.company().getId() != null) {
            Company company = companyRepository.findById(deviceDTO.company().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
            device.setCompany(company);
        } else {
            device.setCompany(null);
        }

        Device updatedDevice = deviceRepository.save(device);
        return convertToDTO(updatedDevice);
    }

    @Override
    public void deleteDevice(Long id) {
        deviceRepository.deleteById(id);
    }

    @Override
    public void deleteByUuid(String uuid) {
        deviceRepository.findOptionalByUuid(uuid).ifPresent(deviceRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceDTO> getAdvicesForDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return device.getAdvices().stream()
                .sorted(Comparator.comparing(Advice::getId))
                .map(AdviceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void assignAdviceToDevice(Long deviceId, Long adviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Advice not found"));
        device.getAdvices().add(advice);
        deviceRepository.save(device);
    }

    @Override
    public void removeAdviceFromDevice(Long deviceId, Long adviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Advice not found"));
        device.getAdvices().remove(advice);
        deviceRepository.save(device);
    }

    private DeviceDTO convertToDTO(Device device) {
        return new DeviceDTO(
                device.getId(),
                device.getUuid(),
                device.getDescriptionName(),
                device.getWidth(),
                device.getHeight(),
                device.getType(),
                device.getCompany());
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/DeviceTypeService.java
package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

public interface DeviceTypeService {
    List<DeviceTypeDTO> getAllDeviceTypes();

    Optional<DeviceTypeDTO> getDeviceTypeById(Long id);

    DeviceTypeDTO saveDeviceType(DeviceTypeDTO DeviceTypeDTO);

    DeviceTypeDTO updateDeviceType(Long id, DeviceTypeDTO DeviceTypeDTO);

    void deleteDeviceType(Long id);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/DeviceTypeServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

@Service
public class DeviceTypeServiceImpl implements DeviceTypeService {

    private final DeviceTypeRepository deviceTypeRepository;

    public DeviceTypeServiceImpl(DeviceTypeRepository deviceTypeRepository) {
        this.deviceTypeRepository = deviceTypeRepository;
    }

    @Override
    public List<DeviceTypeDTO> getAllDeviceTypes() {
        return deviceTypeRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(DeviceTypeDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DeviceTypeDTO> getDeviceTypeById(Long id) {
        return deviceTypeRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public DeviceTypeDTO saveDeviceType(DeviceTypeDTO deviceTypeDTO) {
        DeviceType deviceType = convertToEntity(deviceTypeDTO);
        Optional<DeviceType> exist = deviceTypeRepository.findByType(deviceType.getType());
        if (exist.isPresent())
            return convertToDTO(exist.get());
        DeviceType savedDeviceType = deviceTypeRepository.save(deviceType);
        return convertToDTO(savedDeviceType);
    }

    @Override
    public DeviceTypeDTO updateDeviceType(Long id, DeviceTypeDTO deviceTypeDTO) {
        DeviceType deviceType = deviceTypeRepository.findById(id).orElseThrow();
        deviceType.setType(deviceTypeDTO.type());
        deviceType.setEnabled(deviceTypeDTO.enabled());

        DeviceType updatedDeviceType = deviceTypeRepository.save(deviceType);
        return convertToDTO(updatedDeviceType);
    }

    @Override
    public void deleteDeviceType(Long id) {
        deviceTypeRepository.deleteById(id);
    }

    // Convert DeviceType Entity to DeviceTypeDTO
    private DeviceTypeDTO convertToDTO(DeviceType deviceType) {
        return new DeviceTypeDTO(deviceType.getId(), deviceType.getType(), deviceType.getEnabled());
    }

    // Convert DeviceTypeDTO to DeviceType Entity
    private DeviceType convertToEntity(DeviceTypeDTO deviceTypeDTO) {
        DeviceType deviceType = new DeviceType();
        deviceType.setId(deviceTypeDTO.id());
        deviceType.setType(deviceTypeDTO.type());
        deviceType.setEnabled(deviceTypeDTO.enabled());
        return deviceType;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/FirebaseStorageService.java
package com.screenleads.backend.app.application.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class FirebaseStorageService {

    public String upload(File file, String destination) throws IOException {
        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.create(destination, new FileInputStream(file), Files.probeContentType(file.toPath()));

        // Ya no se puede hacer blob.createAcl(...) si uniform bucket-level access está
        // activado.

        // Devuelve la URL pública si el bucket permite acceso público, o una URL
        // firmada si no.
        return String.format("https://storage.googleapis.com/%s/%s", bucket.getName(), blob.getName());
    }

    public boolean exists(String path) {
        return StorageClient.getInstance().bucket().get(path) != null;
    }

    public String getPublicUrl(String path) {
        return "https://storage.googleapis.com/" + StorageClient.getInstance().bucket().getName() + "/" + path;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/MediaService.java
package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.MediaDTO;

public interface MediaService {
    List<MediaDTO> getAllMedias();

    Optional<MediaDTO> getMediaById(Long id);

    MediaDTO saveMedia(MediaDTO MediaDTO);

    MediaDTO updateMedia(Long id, MediaDTO MediaDTO);

    void deleteMedia(Long id);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/MediaServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.MediaDTO;

@Service
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final MediaTypeRepository mediaTypeRepository;

    public MediaServiceImpl(MediaRepository mediaRepository, MediaTypeRepository mediaTypeRepository) {
        this.mediaRepository = mediaRepository;
        this.mediaTypeRepository = mediaTypeRepository;
    }

    @Override
    public List<MediaDTO> getAllMedias() {
        return mediaRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(MediaDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MediaDTO> getMediaById(Long id) {
        return mediaRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public MediaDTO saveMedia(MediaDTO mediaDTO) {
        Media media = convertToEntity(mediaDTO);
        Media savedMedia = mediaRepository.save(media);
        return convertToDTO(savedMedia);
    }

    @Override
    public MediaDTO updateMedia(Long id, MediaDTO mediaDTO) {
        Media media = mediaRepository.findById(id).orElseThrow();
        media.setSrc(mediaDTO.src());
        media.setType(mediaDTO.type());
        Media updatedMedia = mediaRepository.save(media);
        return convertToDTO(updatedMedia);
    }

    @Override
    public void deleteMedia(Long id) {
        mediaRepository.deleteById(id);
    }

    // Convert Media Entity to MediaDTO
    private MediaDTO convertToDTO(Media media) {
        return new MediaDTO(media.getId(), media.getSrc(), media.getType());
    }

    // Convert MediaDTO to Media Entity
    private Media convertToEntity(MediaDTO mediaDTO) {
        Media media = new Media();
        media.setId(mediaDTO.id());
        media.setSrc(mediaDTO.src());
        // media.setType(mediaDTO.type());
        media.setType(mediaTypeRepository.findById(mediaDTO.type().getId()).get());
        return media;
    }

}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/MediaTypeService.java
package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.MediaTypeDTO;

public interface MediaTypeService {
    List<MediaTypeDTO> getAllMediaTypes();

    Optional<MediaTypeDTO> getMediaTypeById(Long id);

    MediaTypeDTO saveMediaType(MediaTypeDTO MediaTypeDTO);

    MediaTypeDTO updateMediaType(Long id, MediaTypeDTO MediaTypeDTO);

    void deleteMediaType(Long id);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/MediaTypeServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.MediaTypeDTO;

@Service
public class MediaTypeServiceImpl implements MediaTypeService {

    private final MediaTypeRepository mediaTypeRepository;

    public MediaTypeServiceImpl(MediaTypeRepository mediaTypeRepository) {
        this.mediaTypeRepository = mediaTypeRepository;
    }

    @Override
    public List<MediaTypeDTO> getAllMediaTypes() {
        return mediaTypeRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(MediaTypeDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MediaTypeDTO> getMediaTypeById(Long id) {
        return mediaTypeRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public MediaTypeDTO saveMediaType(MediaTypeDTO mediaTypeDTO) {
        MediaType mediaType = convertToEntity(mediaTypeDTO);
        Optional<MediaType> existedByExtension = mediaTypeRepository.findByExtension(mediaType.getExtension());
        Optional<MediaType> existedByType = mediaTypeRepository.findByType(mediaType.getType());
        if (existedByExtension.isPresent())
            return convertToDTO(existedByExtension.get());
        if (existedByType.isPresent())
            return convertToDTO(existedByType.get());
        MediaType savedMediaType = mediaTypeRepository.save(mediaType);
        return convertToDTO(savedMediaType);
    }

    @Override
    public MediaTypeDTO updateMediaType(Long id, MediaTypeDTO mediaTypeDTO) {
        MediaType mediaType = mediaTypeRepository.findById(id).orElseThrow();
        mediaType.setEnabled(mediaTypeDTO.enabled());
        mediaType.setExtension(mediaTypeDTO.extension());
        mediaType.setType(mediaTypeDTO.type());
        MediaType updatedMediaType = mediaTypeRepository.save(mediaType);
        return convertToDTO(updatedMediaType);
    }

    @Override
    public void deleteMediaType(Long id) {
        mediaTypeRepository.deleteById(id);
    }

    // Convert MediaType Entity to MediaTypeDTO
    private MediaTypeDTO convertToDTO(MediaType mediaType) {
        return new MediaTypeDTO(mediaType.getId(), mediaType.getExtension(), mediaType.getType(),
                mediaType.getEnabled());
    }

    // Convert MediaTypeDTO to MediaType Entity
    private MediaType convertToEntity(MediaTypeDTO mediaTypeDTO) {
        MediaType mediaType = new MediaType();
        mediaType.setId(mediaTypeDTO.id());
        mediaType.setExtension(mediaTypeDTO.extension());
        mediaType.setType(mediaTypeDTO.type());
        mediaType.setEnabled(mediaTypeDTO.enabled());
        return mediaType;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/MetadataService.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.EntityInfo;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Lista metadata de entidades con filtro:
 * - Deben tener repositorio (CRUD) registrado.
 * - El usuario actual debe tener permiso de edición (create/update/delete)
 * sobre la entidad.
 */
@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    private final Repositories repositories;
    private final PermissionService perm; // <- tu servicio de permisos

    // Ajusta los paquetes base si lo necesitas
    private static final List<String> BASE_PACKAGES = Arrays.asList("com.screenleads", "com.sl");

    public MetadataService(ApplicationContext applicationContext, PermissionService permissionService) {
        this.repositories = new Repositories(applicationContext);
        this.perm = permissionService;
    }

    public List<EntityInfo> getAllEntities(boolean withCount) {
        List<EntityInfo> list = new ArrayList<>();

        // 1) Intentar obtener domainTypes desde los repositorios
        Set<Class<?>> domainTypes = getRepositoryDomainTypes();

        // 2) Fallback: escanear @Entity en el classpath si no se encontró nada
        if (domainTypes.isEmpty()) {
            domainTypes.addAll(scanEntitiesOnClasspath());
            log.info("Metadata fallback: encontradas {} entidades via escaneo @Entity", domainTypes.size());
        } else {
            log.info("Metadata: encontradas {} entidades via Repositories", domainTypes.size());
        }

        for (Class<?> domainType : domainTypes) {
            try {
                final String fqn = domainType.getName();
                if (!startsWithAny(fqn, BASE_PACKAGES))
                    continue;

                // Debe existir un repositorio CRUD para ser editable en la app
                if (repositories.getRepositoryFor(domainType).isEmpty()) {
                    log.debug("Entidad {} ignorada (sin repositorio asociado)", fqn);
                    continue;
                }

                final String entityName = domainType.getSimpleName();
                final String permKey = normalizeEntityKey(entityName);

                // ---- FILTRO DE PERMISOS (tu PermissionService) ----
                // Editable si el usuario puede create/update/delete la entidad
                boolean canEdit = perm.can(permKey, "create")
                        || perm.can(permKey, "update")
                        || perm.can(permKey, "delete");
                if (!canEdit) {
                    log.debug("Entidad {} ignorada (usuario sin permiso de edición)", entityName);
                    continue;
                }
                // ---------------------------------------------------

                final String tableName = Optional.ofNullable(domainType.getAnnotation(Table.class))
                        .map(Table::name)
                        .filter(s -> !s.isBlank())
                        .orElse(null);

                final String idType = resolveIdType(domainType);

                // Atributos básicos (fields no estáticos ni transient; incluye heredados)
                Map<String, String> attributes = new LinkedHashMap<>();
                for (Field f : getAllFields(domainType)) {
                    if (Modifier.isStatic(f.getModifiers()))
                        continue;
                    if (Modifier.isTransient(f.getModifiers()))
                        continue;
                    attributes.put(f.getName(), f.getType().getSimpleName());
                }

                Long rowCount = null;
                if (withCount) {
                    // Marcador seguro para no tocar la BD (si quieres contar de verdad, descomenta
                    // abajo)
                    rowCount = -1L;

                    // repositories.getRepositoryFor(domainType).ifPresent(repo -> {
                    // if (repo instanceof org.springframework.data.repository.CrudRepository<?, ?>
                    // cr) {
                    // try { rowCount = cr.count(); } catch (Exception e) { rowCount = -1L; }
                    // }
                    // });
                }

                list.add(new EntityInfo(entityName, fqn, tableName, idType, attributes, rowCount));

            } catch (Exception ex) {
                log.warn("No se pudo construir metadata de {}", domainType, ex);
            }
        }

        list.sort(Comparator.comparing(EntityInfo::getEntityName));
        return list;
    }

    // ----------------- Helpers -----------------

    /**
     * Normaliza el nombre simple de la entidad a la clave que usa tu
     * PermissionService (lowercase).
     */
    private String normalizeEntityKey(String simpleName) {
        // Casos típicos: User -> "user", DeviceType -> "devicetype", AppVersion ->
        // "appversion"
        // Si manejas sufijos como "Entity" en tus clases, elimínalos:
        String s = simpleName;
        if (s.endsWith("Entity")) {
            s = s.substring(0, s.length() - "Entity".length());
        }
        return s.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Intenta obtener los domain types de los repos (compatible con múltiples
     * versiones de Spring Data).
     */
    private Set<Class<?>> getRepositoryDomainTypes() {
        Set<Class<?>> types = new LinkedHashSet<>();

        // A) Intento directo: Repositories implementa Iterable<Class<?>>
        try {
            for (Class<?> c : repositories) {
                types.add(c);
            }
        } catch (Throwable t) {
            log.debug("Repositories no es iterable directamente en esta versión: {}", t.toString());
        }

        // B) Intento por reflexión: método getDomainTypes() (existe en versiones
        // nuevas)
        try {
            Method m = repositories.getClass().getMethod("getDomainTypes");
            Object obj = m.invoke(repositories);
            if (obj instanceof Iterable<?>) {
                for (Object o : (Iterable<?>) obj) {
                    if (o instanceof Class<?>) {
                        types.add((Class<?>) o);
                    }
                }
            }
        } catch (NoSuchMethodException ignore) {
            // método no existe en esta versión → OK
        } catch (Exception ex) {
            log.debug("Error invocando getDomainTypes() por reflexión: {}", ex.toString());
        }

        return types;
    }

    /**
     * Escanea el classpath en busca de clases anotadas con @Entity dentro de
     * BASE_PACKAGES.
     */
    private Set<Class<?>> scanEntitiesOnClasspath() {
        Set<Class<?>> result = new LinkedHashSet<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        for (String basePackage : BASE_PACKAGES) {
            try {
                for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
                    String className = bd.getBeanClassName();
                    if (className == null)
                        continue;
                    try {
                        Class<?> clazz = Class.forName(className);
                        result.add(clazz);
                    } catch (ClassNotFoundException e) {
                        log.warn("No se pudo cargar clase escaneada: {}", className);
                    }
                }
            } catch (Exception e) {
                log.warn("Fallo escaneando paquete {}: {}", basePackage, e.toString());
            }
        }
        return result;
    }

    private boolean startsWithAny(String fqn, List<String> prefixes) {
        for (String p : prefixes) {
            if (fqn.startsWith(p + "."))
                return true;
        }
        return false;
    }

    private String resolveIdType(Class<?> domainType) {
        for (Field f : getAllFields(domainType)) {
            if (f.isAnnotationPresent(Id.class)) {
                return f.getType().getSimpleName();
            }
            if (f.isAnnotationPresent(EmbeddedId.class)) {
                return f.getType().getSimpleName() + " (EmbeddedId)";
            }
        }
        return "UnknownId";
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/PermissionService.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("perm")
public class PermissionService {

    private final UserRepository userRepo;

    public PermissionService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public boolean can(String entity, String action) {
        User u = currentUser();
        if (u == null)
            return false;
        for (Role r : u.getRoles()) {
            if (switch (entity.toLowerCase()) {
                case "user" -> switch (action.toLowerCase()) {
                    case "read" -> r.isUserRead();
                    case "create" -> r.isUserCreate();
                    case "update" -> r.isUserUpdate();
                    case "delete" -> r.isUserDelete();
                    default -> false;
                };
                case "company" -> switch (action.toLowerCase()) {
                    case "read" -> r.isCompanyRead();
                    case "create" -> r.isCompanyCreate();
                    case "update" -> r.isCompanyUpdate();
                    case "delete" -> r.isCompanyDelete();
                    default -> false;
                };
                case "device" -> switch (action.toLowerCase()) {
                    case "read" -> r.isDeviceRead();
                    case "create" -> r.isDeviceCreate();
                    case "update" -> r.isDeviceUpdate();
                    case "delete" -> r.isDeviceDelete();
                    default -> false;
                };
                case "devicetype" -> switch (action.toLowerCase()) {
                    case "read" -> r.isDeviceTypeRead();
                    case "create" -> r.isDeviceTypeCreate();
                    case "update" -> r.isDeviceTypeUpdate();
                    case "delete" -> r.isDeviceTypeDelete();
                    default -> false;
                };
                case "media" -> switch (action.toLowerCase()) {
                    case "read" -> r.isMediaRead();
                    case "create" -> r.isMediaCreate();
                    case "update" -> r.isMediaUpdate();
                    case "delete" -> r.isMediaDelete();
                    default -> false;
                };
                case "mediatype" -> switch (action.toLowerCase()) {
                    case "read" -> r.isMediaTypeRead();
                    case "create" -> r.isMediaTypeCreate();
                    case "update" -> r.isMediaTypeUpdate();
                    case "delete" -> r.isMediaTypeDelete();
                    default -> false;
                };
                case "promotion" -> switch (action.toLowerCase()) {
                    case "read" -> r.isPromotionRead();
                    case "create" -> r.isPromotionCreate();
                    case "update" -> r.isPromotionUpdate();
                    case "delete" -> r.isPromotionDelete();
                    default -> false;
                };
                case "advice" -> switch (action.toLowerCase()) {
                    case "read" -> r.isAdviceRead();
                    case "create" -> r.isAdviceCreate();
                    case "update" -> r.isAdviceUpdate();
                    case "delete" -> r.isAdviceDelete();
                    default -> false;
                };
                case "appversion" -> switch (action.toLowerCase()) {
                    case "read" -> r.isAppVersionRead();
                    case "create" -> r.isAppVersionCreate();
                    case "update" -> r.isAppVersionUpdate();
                    case "delete" -> r.isAppVersionDelete();
                    default -> false;
                };
                default -> false;
            })
                return true;
        }
        return false;
    }

    public Integer effectiveLevel() {
        User u = currentUser();
        if (u == null || u.getRoles().isEmpty())
            return Integer.MAX_VALUE;
        return u.getRoles().stream().map(Role::getLevel).min(Integer::compareTo).orElse(Integer.MAX_VALUE);
    }

    private User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated())
            return null;
        Object p = a.getPrincipal();
        if (p instanceof User u)
            return u;
        if (p instanceof org.springframework.security.core.userdetails.User ud) {
            return userRepo.findByUsername(ud.getUsername()).orElse(null);
        }
        if (p instanceof String username) {
            return userRepo.findByUsername(username).orElse(null);
        }
        return null;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/PromotionService.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.*;

import java.time.ZonedDateTime;
import java.util.List;

public interface PromotionService {

    // CRUD promotion
    List<PromotionDTO> getAllPromotions();

    PromotionDTO getPromotionById(Long id);

    PromotionDTO savePromotion(PromotionDTO dto);

    PromotionDTO updatePromotion(Long id, PromotionDTO dto);

    void deletePromotion(Long id);

    // Leads
    PromotionLeadDTO registerLead(Long promotionId, PromotionLeadDTO dto);

    List<PromotionLeadDTO> listLeads(Long promotionId);

    // Informes / export
    String exportLeadsCsv(Long promotionId, ZonedDateTime from, ZonedDateTime to);

    LeadSummaryDTO getLeadSummary(Long promotionId, ZonedDateTime from, ZonedDateTime to);

    // Lead de prueba
    PromotionLeadDTO createTestLead(Long promotionId, PromotionLeadDTO overrides);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/PromotionServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.application.service.util.IdentifierNormalizer;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import com.screenleads.backend.app.domain.model.LeadLimitType;
import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.domain.model.PromotionLead;
import com.screenleads.backend.app.domain.repositories.PromotionLeadRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl {

    private final PromotionRepository promotionRepository;
    private final PromotionLeadRepository promotionLeadRepository;

    // =========================================================================
    //                               PROMOTIONS
    // =========================================================================

    public Promotion createPromotion(Promotion dto) {
        // Si usas DTOs separados, mapéalos antes.
        Promotion p = Promotion.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .legalUrl(dto.getLegalUrl())           // camelCase correcto
                .url(dto.getUrl())
                .templateHtml(dto.getTemplateHtml())
                .leadIdentifierType(dto.getLeadIdentifierType())
                .leadLimitType(defaultLimit(dto.getLeadLimitType()))
                .company(dto.getCompany())
                .build();
        return promotionRepository.save(p);
    }

    public Promotion updatePromotion(Long id, Promotion dto) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));

        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setLegalUrl(dto.getLegalUrl());
        p.setUrl(dto.getUrl());
        p.setTemplateHtml(dto.getTemplateHtml());

        if (dto.getLeadIdentifierType() != null) {
            p.setLeadIdentifierType(dto.getLeadIdentifierType());
        }
        if (dto.getLeadLimitType() != null) {
            p.setLeadLimitType(defaultLimit(dto.getLeadLimitType()));
        }
        // Entidad gestionada: se sincroniza al commit
        return p;
    }

    // =========================================================================
    //                                  LEADS
    // =========================================================================

    public PromotionLead addLead(Long promotionId, PromotionLead payload) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));

        // Resolver tipos con fallback a la promoción (y defaults)
        LeadIdentifierType resolvedIdType = resolveIdentifierType(payload.getIdentifierType(), promo.getLeadIdentifierType());
        LeadLimitType resolvedLimitType   = resolveLimitType(payload.getLimitType(), promo.getLeadLimitType());

        // Normalizar el identifier según tipo
        String rawIdentifier = payload.getIdentifier();
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            throw new IllegalArgumentException("identifier is required");
        }
        String normalizedIdentifier = IdentifierNormalizer.normalize(resolvedIdType, rawIdentifier);

        // Pre-chequeo de unicidad para mensaje de error amistoso
        if (promotionLeadRepository.existsByPromotionIdAndIdentifier(promotionId, normalizedIdentifier)) {
            throw new IllegalStateException("Lead already exists for this promotion and identifier");
        }

        PromotionLead lead = PromotionLead.builder()
                .promotion(promo)
                // Datos personales (opcional según tu modelo/uso)
                .firstName(payload.getFirstName())
                .lastName(payload.getLastName())
                .email(payload.getEmail())
                .phone(payload.getPhone())
                .birthDate(payload.getBirthDate())
                .acceptedPrivacyAt(payload.getAcceptedPrivacyAt())
                .acceptedTermsAt(payload.getAcceptedTermsAt())
                // Identificador + límites resueltos/normalizados
                .identifierType(resolvedIdType)
                .identifier(normalizedIdentifier)
                .limitType(resolvedLimitType)
                .build();

        try {
            return promotionLeadRepository.save(lead);
        } catch (DataIntegrityViolationException dive) {
            // Por si otro proceso ganó la carrera y saltó la unique constraint DB
            throw new IllegalStateException("Lead already exists for this promotion and identifier", dive);
        }
    }

    @Transactional(readOnly = true)
    public TreeMap<ZonedDateTime, Long> leadsPerDay(Long promotionId, ZoneId zoneId) {
        ZoneId zone = Objects.requireNonNullElse(zoneId, ZoneId.systemDefault());
        List<PromotionLead> leads = promotionLeadRepository.findByPromotionId(promotionId);

        return leads.stream().collect(Collectors.groupingBy(
                l -> l.getCreatedAt()                 // Instant (Auditable)
                        .atZone(zone)
                        .toLocalDate()
                        .atStartOfDay(zone),
                TreeMap::new,
                Collectors.counting()
        ));
    }

    // =========================================================================
    //                               HELPERS
    // =========================================================================

    @Transactional(readOnly = true)
    public LeadIdentifierType getIdentifierType(Long promotionId) {
        return promotionRepository.findById(promotionId)
                .map(Promotion::getLeadIdentifierType)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));
    }

    @Transactional(readOnly = true)
    public LeadLimitType getLimitType(Long promotionId) {
        return promotionRepository.findById(promotionId)
                .map(Promotion::getLeadLimitType)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));
    }

    private static LeadLimitType defaultLimit(LeadLimitType value) {
        return value != null ? value : LeadLimitType.NO_LIMIT;
    }

    private static LeadIdentifierType resolveIdentifierType(LeadIdentifierType fromPayload,
                                                            LeadIdentifierType fromPromotion) {
        if (fromPayload != null) return fromPayload;
        if (fromPromotion != null) return fromPromotion;
        // Por defecto, si no viene ni en payload ni en promo:
        return LeadIdentifierType.OTHER;
    }

    private static LeadLimitType resolveLimitType(LeadLimitType fromPayload,
                                                  LeadLimitType fromPromotion) {
        if (fromPayload != null) return fromPayload;
        if (fromPromotion != null) return fromPromotion;
        return LeadLimitType.NO_LIMIT;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/RoleService.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.web.dto.RoleDTO;

import java.util.List;

public interface RoleService {
    List<RoleDTO> getAll();

    RoleDTO getById(Long id);

    RoleDTO create(RoleDTO dto);

    RoleDTO update(Long id, RoleDTO dto);

    void delete(Long id);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/RoleServiceImpl.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.application.service.RoleService;
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

```

```java
// src/main/java/com/screenleads/backend/app/application/service/UserService.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> getAll();

    UserDto getById(Long id);

    UserDto create(UserDto dto);

    UserDto update(Long id, UserDto dto);

    void delete(Long id);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/UserServiceImpl.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.UserDto;
import com.screenleads.backend.app.web.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PermissionService perm;
    private final SecureRandom random = new SecureRandom();

    @PersistenceContext
    private EntityManager entityManager;

    public UserServiceImpl(
            UserRepository repo,
            CompanyRepository companyRepo,
            RoleRepository roleRepo,
            PasswordEncoder passwordEncoder,
            PermissionService perm) {
        this.repo = repo;
        this.companyRepo = companyRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.perm = perm;
    }

    // ---------- LECTURAS (activamos filtro en la misma Session/Tx) ----------

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll() {
        enableCompanyFilterIfNeeded();
        return repo.findAll().stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        enableCompanyFilterIfNeeded();
        return repo.findById(id)
                .map(UserMapper::toDto)
                .orElse(null);
    }

    // ---------------------------- ESCRITURAS ----------------------------

    @Override
    @Transactional
    public void delete(Long id) {
        enableCompanyFilterIfNeeded(); // protege que no borre fuera de su compañía
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public UserDto create(UserDto dto) {
        if (dto == null)
            throw new IllegalArgumentException("Body requerido");
        if (dto.getUsername() == null || dto.getUsername().isBlank())
            throw new IllegalArgumentException("username requerido");

        // Permisos para crear y jerarquía de nivel 1 o 2
        assertCanCreateUser();

        repo.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("username ya existe");
        });

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setName(dto.getName());
        u.setLastName(dto.getLastName());

        // Generación/establecimiento de password
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().isBlank())
                ? dto.getPassword()
                : generateTempPassword(12);
        u.setPassword(passwordEncoder.encode(rawPassword));

        // Company
        if (dto.getCompanyId() != null) {
            Company c = companyRepo.findById(dto.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + dto.getCompanyId()));
            u.setCompany(c);
        } else {
            // Si no viene companyId y el usuario no es admin, forzar su compañía
            Long currentCompanyId = currentCompanyId();
            if (currentCompanyId != null && !isCurrentUserAdmin()) {
                Company c = companyRepo.findById(currentCompanyId)
                        .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + currentCompanyId));
                u.setCompany(c);
            }
        }

        // Roles
        Set<Role> roles = new HashSet<>();
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            for (String rn : dto.getRoles()) {
                Role r = roleRepo.findByRole(rn)
                        .orElseThrow(() -> new IllegalArgumentException("role inválido: " + rn));
                roles.add(r);
            }
        }
        if (roles.isEmpty())
            throw new IllegalArgumentException("Se requiere al menos un rol");

        // Verificación de jerarquía: no puedes asignar un nivel superior al tuyo
        assertAssignableRoles(roles);

        u.setRoles(roles);

        User saved = repo.save(u);
        return UserMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto dto) {
        enableCompanyFilterIfNeeded(); // asegura que solo se actualicen usuarios de su compañía (no admin)

        return repo.findById(id).map(existing -> {
            if (dto.getUsername() != null)
                existing.setUsername(dto.getUsername());
            if (dto.getEmail() != null)
                existing.setEmail(dto.getEmail());
            if (dto.getName() != null)
                existing.setName(dto.getName());
            if (dto.getLastName() != null)
                existing.setLastName(dto.getLastName());

            // Permitir cambio de password
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            // Cambiar compañía (si se permite)
            if (dto.getCompanyId() != null) {
                // Si el actual no es admin, no permitir cambiar a otra compañía distinta de la
                // suya
                if (!isCurrentUserAdmin()) {
                    Long currentCompanyId = currentCompanyId();
                    if (currentCompanyId == null || !currentCompanyId.equals(dto.getCompanyId())) {
                        throw new IllegalArgumentException("No autorizado a cambiar de compañía");
                    }
                }
                Company c = companyRepo.findById(dto.getCompanyId())
                        .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + dto.getCompanyId()));
                existing.setCompany(c);
            }

            // Roles (si vienen, aplicar mismas reglas de permiso y jerarquía)
            if (dto.getRoles() != null) {
                if (!perm.can("user", "update")) {
                    throw new IllegalArgumentException("No autorizado a actualizar usuarios");
                }
                Set<Role> roles = new HashSet<>();
                for (String rn : dto.getRoles()) {
                    Role r = roleRepo.findByRole(rn)
                            .orElseThrow(() -> new IllegalArgumentException("role inválido: " + rn));
                    roles.add(r);
                }
                if (roles.isEmpty())
                    throw new IllegalArgumentException("Se requiere al menos un rol");
                assertAssignableRoles(roles);
                existing.setRoles(roles);
            }

            User saved = repo.save(existing);
            return UserMapper.toDto(saved);
        }).orElse(null);
    }

    // ---------------------------- HELPERS ----------------------------

    /**
     * Activa el filtro "companyFilter" en la misma Session/Tx usada por el
     * repositorio
     * cuando el usuario actual NO es admin.
     */
    private void enableCompanyFilterIfNeeded() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return;

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
        if (isAdmin)
            return;

        Long companyId = resolveCompanyId(auth);
        if (companyId == null)
            return;

        Session session = entityManager.unwrap(Session.class);
        var filter = session.getEnabledFilter("companyFilter");
        if (filter == null) {
            session.enableFilter("companyFilter").setParameter("companyId", companyId);
        } else {
            filter.setParameter("companyId", companyId);
        }
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
    }

    private Long currentCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        return resolveCompanyId(auth);
    }

    private Long resolveCompanyId(Authentication auth) {
        Object principal = auth.getPrincipal();

        // 1) Entidad de dominio como principal
        if (principal instanceof User u) {
            return (u.getCompany() != null) ? u.getCompany().getId() : null;
        }

        // 2) UserDetails estándar
        if (principal instanceof UserDetails ud) {
            return repo.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        // 3) Principal como String (username), p.ej. JWT con "sub"
        if (principal instanceof String username) {
            return repo.findByUsername(username)
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        return null;
    }

    private String generateTempPassword(int length) {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return sb.toString();
    }

    // ======== NUEVOS HELPERS DE PERMISOS/NIVELES ========

    private int currentEffectiveLevel() {
        return perm.effectiveLevel();
    }

    private void assertCanCreateUser() {
        if (!perm.can("user", "create"))
            throw new IllegalArgumentException("No autorizado a crear usuarios");
        int L = currentEffectiveLevel();
        if (L > 2) // solo niveles 1 o 2 pueden crear
            throw new IllegalArgumentException("Solo roles de nivel 1 o 2 pueden crear usuarios");
    }

    private void assertAssignableRoles(Set<Role> targetRoles) {
        int myLevel = currentEffectiveLevel();
        int newUserLevel = targetRoles.stream().map(Role::getLevel).min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
        if (newUserLevel < myLevel) {
            // ej.: soy nivel 2, intento asignar nivel 1 → prohibido
            throw new IllegalArgumentException("No puedes asignar un rol superior al tuyo");
        }
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/WebSocketService.java
package com.screenleads.backend.app.application.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.screenleads.backend.app.domain.model.ChatMessage;

public interface WebSocketService {
    void WSService(SimpMessagingTemplate messagingTemplate);

    void notifyFrontend(final ChatMessage message, final String roomId);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/WebSocketServiceImpl.java
package com.screenleads.backend.app.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.ChatMessage;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    private SimpMessagingTemplate messagingTemplate = null;

    @Autowired
    public void WSService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyFrontend(final ChatMessage message, final String roomId) {

        System.out.println("Llega aqui" + roomId + message.getMessage());

        messagingTemplate.convertAndSend("/topic/" + roomId, message);
    }

}
```

```java
// src/main/java/com/screenleads/backend/app/application/service/util/CouponCodeGenerator.java
package com.screenleads.backend.app.application.service.util;

import java.security.SecureRandom;

public final class CouponCodeGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sin 0/O/I/1 para evitar confusión
    private static final SecureRandom RAND = new SecureRandom();

    private CouponCodeGenerator() {}

    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RAND.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/util/IdentifierNormalizer.java
package com.screenleads.backend.app.application.service.util;

import com.screenleads.backend.app.domain.model.LeadIdentifierType;

public final class IdentifierNormalizer {

    private IdentifierNormalizer() {}

    public static String normalize(LeadIdentifierType type, String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        switch (type) {
            case EMAIL:
                return s.toLowerCase();
            case PHONE:
                // Normalización simple: quitar espacios/guiones, convertir 00xx a +xx
                String p = s.replaceAll("[^+\\d]", "");
                if (p.startsWith("00")) {
                    p = "+" + p.substring(2);
                }
                return p;
            case DOCUMENT:
                return s.replaceAll("\\s+", "").toUpperCase();
            case OTHER:
            default:
                return s;
        }
    }
}

```

