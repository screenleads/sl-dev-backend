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
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.*;

@Service
public class AdviceServiceImpl implements AdviceService {

    private final AdviceRepository adviceRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final MediaTypeRepository mediaTypeRepository;
    private final CompanyRepository companyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AdviceServiceImpl(AdviceRepository adviceRepository,
                             MediaRepository mediaRepository,
                             UserRepository userRepository,
                             MediaTypeRepository mediaTypeRepository,
                             CompanyRepository companyRepository) {
        this.adviceRepository = adviceRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.mediaTypeRepository = mediaTypeRepository;
        this.companyRepository = companyRepository;
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
                    && (s.getEndDate() == null || !date.isAfter(s.getEndDate()));
            if (!dateOk)
                continue;

            // Si el día no tiene horas, no es visible ese día
            if (s.getWindows() == null || s.getWindows().isEmpty())
                continue;

            boolean any = s.getWindows().stream().anyMatch(w -> w.getWeekday() == weekday
                    && w.getFromTime() != null && w.getToTime() != null
                    && !time.isBefore(w.getFromTime()) // >= from
                    && time.isBefore(w.getToTime()) // < to (fin exclusivo)
            );
            if (any)
                return true;
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

        System.out.println("[DEBUG] AdviceDTO recibido: " + dto);

        Advice advice = new Advice();
        advice.setDescription(dto.getDescription());
        advice.setCustomInterval(Boolean.TRUE.equals(dto.getCustomInterval()));
        advice.setInterval(numberToDuration(dto.getInterval()));

        advice.setCompany(resolveCompanyForWrite(dto.getCompany(), null));
        // Si la media no existe, crearla con los datos mínimos
        Media media = null;
        MediaUpsertDTO mediaDto = dto.getMedia();
        if (mediaDto != null) {
            if (mediaDto.id() != null && mediaDto.id() > 0) {
                media = mediaRepository.findById(mediaDto.id()).orElse(null);
            } else if (mediaDto.src() != null && !mediaDto.src().isBlank()) {
                media = mediaRepository.findBySrc(mediaDto.src().trim()).orElse(null);
                if (media == null) {
                    // Crear nueva Media
                    MediaType defaultType = null;
                    Company company = advice.getCompany();
                    // Buscar tipo por extensión
                    String ext = null;
                    String src = mediaDto.src().trim();
                    int dotIdx = src.lastIndexOf('.');
                    if (dotIdx > 0 && dotIdx < src.length() - 1) {
                        ext = src.substring(dotIdx + 1).toLowerCase();
                    }
                    if (ext != null) {
                        defaultType = mediaTypeRepository.findByExtension(ext).orElse(null);
                    }
                    if (defaultType == null) {
                        // fallback: primer tipo disponible
                        defaultType = mediaTypeRepository.findAll().stream().findFirst().orElse(null);
                    }
                    if (company != null && defaultType != null) {
                        Media newMedia = new Media();
                        newMedia.setSrc(src);
                        newMedia.setType(defaultType);
                        newMedia.setCompany(company);
                        media = mediaRepository.save(newMedia);
                    }
                }
            }
        }
        advice.setMedia(media);
        advice.setPromotion(resolvePromotionFromDto(dto.getPromotion()));

        // schedules
        advice.setSchedules(new ArrayList<>());
        if (dto.getSchedules() != null) {
            for (AdviceScheduleDTO sDto : dto.getSchedules()) {
                AdviceSchedule mappedSchedule = mapScheduleDTO(sDto, advice);
                System.out.println("[DEBUG] AdviceSchedule mapeado: startDate=" + mappedSchedule.getStartDate() + ", endDate=" + mappedSchedule.getEndDate());
                if (mappedSchedule.getWindows() != null) {
                    for (AdviceTimeWindow win : mappedSchedule.getWindows()) {
                        System.out.println("[DEBUG] AdviceTimeWindow mapeado: weekday=" + win.getWeekday() + ", from=" + win.getFromTime() + ", to=" + win.getToTime());
                    }
                }
                advice.getSchedules().add(mappedSchedule);
            }
        }

        validateAdvice(advice);
        Advice saved = adviceRepository.save(advice);
            // Log de persistencia real de ventanas
            int totalWindows = saved.getSchedules() == null ? 0 : saved.getSchedules().stream().mapToInt(s -> s.getWindows() == null ? 0 : s.getWindows().size()).sum();
            System.out.println("[DEBUG] Advice guardado. Total ventanas: " + totalWindows);
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
        if (advice.getSchedules() == null)
            return;
        for (AdviceSchedule s : advice.getSchedules()) {
            if (s.getStartDate() != null && s.getEndDate() != null &&
                    s.getStartDate().isAfter(s.getEndDate())) {
                throw new IllegalArgumentException("Schedule startDate must be <= endDate");
            }
            if (s.getWindows() != null)
                validateAndNormalizeWindows(s.getWindows());
        }
    }

    /** from<to, orden por día+hora, no solapes dentro del mismo día. */
    private void validateAndNormalizeWindows(List<AdviceTimeWindow> windows) {
        for (AdviceTimeWindow w : windows) {
            if (w.getWeekday() == null)
                throw new IllegalArgumentException("Window weekday is required");
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
            AdviceTimeWindow cur = windows.get(i);
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
        // Soportar ambos formatos: windows plano y dayWindows agrupado
        if (sDto.getWindows() != null && !sDto.getWindows().isEmpty()) {
            for (AdviceTimeWindowDTO wDto : sDto.getWindows()) {
                AdviceTimeWindow w = new AdviceTimeWindow();
                w.setWeekday(parseWeekday(wDto.getWeekday()));
                w.setFromTime(parseTime(wDto.getFromTime()));
                w.setToTime(parseTime(wDto.getToTime()));
                windows.add(w);
            }
        } else if (sDto.getDayWindows() != null && !sDto.getDayWindows().isEmpty()) {
            for (AdviceScheduleDTO.DayWindowDTO day : sDto.getDayWindows()) {
                if (day.getRanges() != null) {
                    for (AdviceScheduleDTO.RangeDTO range : day.getRanges()) {
                        AdviceTimeWindow w = new AdviceTimeWindow();
                        w.setWeekday(parseWeekday(day.getWeekday()));
                        w.setFromTime(parseTime(range.getFromTime()));
                        w.setToTime(parseTime(range.getToTime()));
                        windows.add(w);
                    }
                }
            }
        }
        validateAndNormalizeWindows(windows);
        for (AdviceTimeWindow w : windows) {
            w.setSchedule(s);
        }
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
                        wins.add(new AdviceTimeWindowDTO(
                                w.getId(),
                                w.getWeekday() != null ? w.getWeekday().name() : null,
                                formatTime(w.getFromTime()),
                                formatTime(w.getToTime())
                        ));
                    }
                }
                schedules.add(new AdviceScheduleDTO(
                        s.getId(),
                        formatDate(s.getStartDate()),
                        formatDate(s.getEndDate()),
                        wins,
                        null
                ));
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
        if (n == null)
            return null;
        long s = n.longValue();
        return (s > 0) ? Duration.ofSeconds(s) : null;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank())
            return null;
        String value = s.trim();
        try {
            // Si viene en formato 'YYYY-MM-DD', parse normal
            if (value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                return LocalDate.parse(value);
            }
            // Si viene en formato ISO con zona (ej: 2025-09-30T22:00:00.000Z)
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Fecha inválida: " + value, ex);
        }
    }

    private String formatDate(LocalDate d) {
        return (d == null) ? null : d.toString();
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank())
            return null;
        String t = s.trim();
        if (t.matches("^\\d{2}:\\d{2}$"))
            t = t + ":00";
        return LocalTime.parse(t, DateTimeFormatter.ISO_LOCAL_TIME);
    }

    private String formatTime(LocalTime t) {
        return (t == null) ? null : t.toString(); // HH:mm:ss
    }

    private DayOfWeek parseWeekday(String s) {
        if (s == null || s.isBlank())
            return null;
        return DayOfWeek.valueOf(s.trim().toUpperCase());
    }

    private Media resolveMediaFromDto(MediaUpsertDTO incoming) {
        if (incoming == null)
            return null;
        if (incoming.id() != null && incoming.id() > 0) {
            return mediaRepository.findById(incoming.id())
                    .orElseThrow(() -> new IllegalArgumentException("Media no encontrada (id=" + incoming.id() + ")"));
        }
        if (incoming.src() != null && !incoming.src().isBlank()) {
            return mediaRepository.findBySrc(incoming.src().trim())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Media no encontrada por src (debe crearse antes)"));
        }
        return null;
    }

    private Promotion resolvePromotionFromDto(PromotionRefDTO incoming) {
        if (incoming == null)
            return null;
        Long id = incoming.id();
        if (id == null || id <= 0)
            return null;
        return entityManager.getReference(Promotion.class, id);
    }

    private Company resolveCompanyForWrite(CompanyRefDTO incoming, Company current) {
        // Si el DTO trae un id válido, buscar y asignar la compañía SIEMPRE
        Long desiredId = (incoming != null) ? incoming.id() : null;
        if (desiredId != null && desiredId > 0) {
            return companyRepository.findById(desiredId).orElse(null);
        }
        // Si no, usar la lógica anterior (usuario actual, current, etc)
        boolean isAdmin = isCurrentUserAdmin();
        Long userCompanyId = currentCompanyId();
        if (!isAdmin) {
            if (userCompanyId != null) {
                return companyRepository.findById(userCompanyId).orElse(null);
            }
            return current;
        }
        if (current != null)
            return current;
        if (userCompanyId != null) {
            return companyRepository.findById(userCompanyId).orElse(null);
        }
        return null;
    }

    /** Activa el filtro "companyFilter" si NO es admin. */
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
// src/main/java/com/screenleads/backend/app/application/service/AppEntityService.java
package com.screenleads.backend.app.application.service;

import java.util.List;

import com.screenleads.backend.app.web.dto.AppEntityDTO;

public interface AppEntityService {

    // ===== Query =====
    List<AppEntityDTO> findAll(boolean withCount);

    AppEntityDTO findById(Long id, boolean withCount);

    AppEntityDTO findByResource(String resource, boolean withCount);

    // ===== Commands =====
    AppEntityDTO upsert(AppEntityDTO dto);

    void deleteById(Long id);

    // ===== Reorder (drag & drop) =====
    /**
     * Reordena las entidades visibles en menú (visibleInMenu=true) con base en
     * la lista de IDs recibida. Resequia sortOrder empezando en 1.
     */
    void reorderEntities(List<Long> orderedIds);

    /**
     * Reordena los atributos de una entidad por la lista de IDs recibida.
     * Actualiza listOrder y, si procede, formOrder para mantener consistencia.
     */
    void reorderAttributes(Long entityId, List<Long> orderedAttributeIds);
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/AppEntityServiceImpl.java
package com.screenleads.backend.app.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;
import com.screenleads.backend.app.domain.repositories.AppEntityRepository;
import com.screenleads.backend.app.web.dto.AppEntityDTO;
import com.screenleads.backend.app.web.dto.EntityAttributeDTO;
import com.screenleads.backend.app.web.mapper.AppEntityMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppEntityServiceImpl implements AppEntityService {

    private final AppEntityRepository repo;

    @Nullable
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(@Nullable JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ===================== QUERY =====================

    @Override
    public List<AppEntityDTO> findAll(boolean withCount) {
        List<AppEntity> all = repo.findAll();

        if (withCount) {
            refreshRowCountsInMemory(all);
        }

        // ordenar por sortOrder si existe, luego por displayLabel/entityName
        all.sort((a, b) -> {
            int sa = a.getSortOrder() == null ? Integer.MAX_VALUE : a.getSortOrder();
            int sb = b.getSortOrder() == null ? Integer.MAX_VALUE : b.getSortOrder();
            if (sa != sb)
                return Integer.compare(sa, sb);
            String la = a.getDisplayLabel() != null ? a.getDisplayLabel() : a.getEntityName();
            String lb = b.getDisplayLabel() != null ? b.getDisplayLabel() : b.getEntityName();
            return la.compareToIgnoreCase(lb);
        });

        return all.stream().map(AppEntityMapper::toDto).toList();
    }

    @Override
    public AppEntityDTO findById(Long id, boolean withCount) {
        AppEntity e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AppEntity no encontrada: id=" + id));
        if (withCount) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
        return AppEntityMapper.toDto(e);
    }

    @Override
    public AppEntityDTO findByResource(String resource, boolean withCount) {
        AppEntity e = repo.findByResource(resource)
                .orElseThrow(() -> new IllegalArgumentException("AppEntity no encontrada: resource=" + resource));
        if (withCount) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
        return AppEntityMapper.toDto(e);
    }

    // ===================== COMMANDS =====================

    @Override
    @Transactional
    public AppEntityDTO upsert(AppEntityDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AppEntityDTO nulo");
        }
        if (dto.resource() == null || dto.resource().isBlank()) {
            throw new IllegalArgumentException("resource obligatorio en AppEntityDTO");
        }

        AppEntity e = repo.findByResource(dto.resource())
                .orElseGet(() -> AppEntity.builder().resource(dto.resource()).build());

        // Metadatos principales
        e.setEntityName(nullIfBlank(dto.entityName(), e.getEntityName()));
        e.setClassName(nullIfBlank(dto.className(), e.getClassName()));
        e.setTableName(nullIfBlank(dto.tableName(), e.getTableName()));
        e.setIdType(nullIfBlank(dto.idType(), e.getIdType()));
        e.setEndpointBase(nullIfBlank(dto.endpointBase(), e.getEndpointBase()));
        e.setCreateLevel(dto.createLevel() != null ? dto.createLevel() : e.getCreateLevel());
        e.setReadLevel(dto.readLevel() != null ? dto.readLevel() : e.getReadLevel());
        e.setUpdateLevel(dto.updateLevel() != null ? dto.updateLevel() : e.getUpdateLevel());
        e.setDeleteLevel(dto.deleteLevel() != null ? dto.deleteLevel() : e.getDeleteLevel());
        e.setVisibleInMenu(dto.visibleInMenu() != null ? dto.visibleInMenu() : e.getVisibleInMenu());
        // Dashboard metadata
        if (dto.displayLabel() != null && !dto.displayLabel().isBlank()) {
            e.setDisplayLabel(dto.displayLabel());
        } else if (e.getDisplayLabel() == null && e.getEntityName() != null) {
            e.setDisplayLabel(e.getEntityName());
        }
        e.setIcon(dto.icon() != null ? dto.icon() : e.getIcon());
        e.setSortOrder(dto.sortOrder() != null ? dto.sortOrder() : e.getSortOrder());

        // Atributos (merge no destructivo)
        if (dto.attributes() != null) {
            mergeAttributes(e, dto.attributes());
        }

        AppEntity saved = repo.save(e);
        return AppEntityMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    // ===================== REORDER (drag & drop) =====================

    @Override
    @Transactional
    public void reorderEntities(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("Debe enviarse una lista de IDs para reordenar.");
        }

        // Validar duplicados en la lista
        var seen = new HashSet<Long>();
        for (Long id : orderedIds) {
            if (id == null) {
                throw new IllegalArgumentException("La lista de IDs no puede contener nulos.");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("La lista de IDs contiene duplicados: " + id);
            }
        }

        // Reordenamos SOLO las visibles en menú
        List<AppEntity> visibles = repo.findByVisibleInMenuTrueOrderBySortOrderAsc();
        if (visibles.isEmpty())
            return;

        Map<Long, AppEntity> byId = new HashMap<>();
        for (AppEntity e : visibles) {
            byId.put(e.getId(), e);
        }

        // Validar que todos los IDs pertenezcan al conjunto visible
        for (Long id : orderedIds) {
            if (!byId.containsKey(id)) {
                throw new IllegalArgumentException("El ID " + id + " no pertenece a entidades visibles en menú.");
            }
        }

        int order = 1;
        // Asignar primero los recibidos en el nuevo orden
        for (Long id : orderedIds) {
            AppEntity e = byId.remove(id);
            if (e != null)
                e.setSortOrder(order++);
        }

        // Mantener el resto con su orden relativo, colocándolos a continuación
        for (AppEntity e : visibles) {
            if (byId.containsKey(e.getId())) {
                e.setSortOrder(order++);
            }
        }

        repo.saveAll(visibles);
    }

    @Override
    @Transactional
    public void reorderAttributes(Long entityId, List<Long> orderedAttributeIds) {
        if (entityId == null)
            throw new IllegalArgumentException("entityId requerido");
        if (orderedAttributeIds == null || orderedAttributeIds.isEmpty()) {
            throw new IllegalArgumentException("Debe enviarse una lista de IDs de atributos para reordenar.");
        }

        var seen = new HashSet<Long>();
        for (Long id : orderedAttributeIds) {
            if (id == null) {
                throw new IllegalArgumentException("La lista de IDs de atributos no puede contener nulos.");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("La lista de IDs de atributos contiene duplicados: " + id);
            }
        }

        AppEntity entity = repo.findWithAttributesById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("AppEntity no encontrada: id=" + entityId));

        if (entity.getAttributes() == null || entity.getAttributes().isEmpty())
            return;

        Map<Long, AppEntityAttribute> byId = new HashMap<>();
        for (AppEntityAttribute a : entity.getAttributes()) {
            if (a.getId() != null)
                byId.put(a.getId(), a);
        }

        // Validar pertenencia
        for (Long id : orderedAttributeIds) {
            if (!byId.containsKey(id)) {
                throw new IllegalArgumentException("El atributo " + id + " no pertenece a la entidad " + entityId);
            }
        }

        int order = 0;

        // Reordena los indicados primero
        for (Long id : orderedAttributeIds) {
            AppEntityAttribute a = byId.remove(id);
            if (a != null) {
                int newOrder = ++order;
                Integer oldListOrder = a.getListOrder();
                a.setListOrder(newOrder);

                // Solo sincroniza formOrder si no estaba personalizado
                if (a.getFormOrder() == null || (oldListOrder != null && a.getFormOrder().equals(oldListOrder))) {
                    a.setFormOrder(newOrder);
                }
            }
        }

        // Añade el resto manteniendo su orden relativo original
        List<AppEntityAttribute> remaining = entity.getAttributes().stream()
                .filter(a -> byId.containsKey(a.getId()))
                .sorted((x, y) -> {
                    Integer lx = x.getListOrder() == null ? Integer.MAX_VALUE : x.getListOrder();
                    Integer ly = y.getListOrder() == null ? Integer.MAX_VALUE : y.getListOrder();
                    return Integer.compare(lx, ly);
                })
                .toList();

        for (AppEntityAttribute a : remaining) {
            int newOrder = ++order;
            Integer oldListOrder = a.getListOrder();
            a.setListOrder(newOrder);
            if (a.getFormOrder() == null || (oldListOrder != null && a.getFormOrder().equals(oldListOrder))) {
                a.setFormOrder(newOrder);
            }
        }

        // Guardar. (Cascade en AppEntity -> AppEntityAttribute)
        repo.save(entity);
    }

    // ===================== ROW COUNT =====================

    private void refreshRowCountsInMemory(List<AppEntity> entities) {
        for (AppEntity e : entities) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
    }

    private Long countRowsSafe(String tableName) {
        if (tableName == null || tableName.isBlank() || jdbcTemplate == null)
            return null;
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        } catch (Exception ex) {
            return null;
        }
    }

    // ===================== ATTRIBUTES MERGE/REPLACE =====================

    /**
     * Merge NO destructivo: actualiza los existentes (por id o name) y crea nuevos.
     * No borra.
     */
    private void mergeAttributes(AppEntity e, List<EntityAttributeDTO> incoming) {
        if (e.getAttributes() == null) {
            e.setAttributes(new ArrayList<>());
        }

        // Índices para localizar rápido existentes
        Map<Long, AppEntityAttribute> byId = new HashMap<>();
        Map<String, AppEntityAttribute> byName = new HashMap<>();
        for (AppEntityAttribute a : e.getAttributes()) {
            if (a.getId() != null)
                byId.put(a.getId(), a);
            if (a.getName() != null)
                byName.put(a.getName(), a);
        }

        for (EntityAttributeDTO dto : incoming) {
            AppEntityAttribute target = null;

            if (dto.id() != null) {
                target = byId.get(dto.id());
            }
            if (target == null && dto.name() != null) {
                target = byName.get(dto.name());
            }

            if (target == null) {
                // crear nuevo
                target = new AppEntityAttribute();
                target.setAppEntity(e);
                applyAttrDto(target, dto);
                e.getAttributes().add(target);
            } else {
                // actualizar existente (campos null no pisan)
                applyAttrDto(target, dto);
            }
        }
    }

    /**
     * Reemplazo exacto: deja exactamente los atributos recibidos (borra los demás).
     */
    @SuppressWarnings("unused")
    private void replaceAttributes(AppEntity e, List<EntityAttributeDTO> incoming) {
        if (e.getAttributes() == null) {
            e.setAttributes(new ArrayList<>());
        } else {
            e.getAttributes().clear();
        }
        for (EntityAttributeDTO dto : incoming) {
            AppEntityAttribute a = new AppEntityAttribute();
            a.setAppEntity(e);
            applyAttrDto(a, dto);
            e.getAttributes().add(a);
        }
    }

    /** Copia segura: sólo pisa si el DTO trae valor no nulo. */
    private void applyAttrDto(AppEntityAttribute a, EntityAttributeDTO d) {
        AppEntityMapper.applyAttrDto(a, d);
    }

    private static String nullIfBlank(String v, String fallback) {
        if (v == null)
            return fallback;
        String t = v.trim();
        return t.isEmpty() ? fallback : t;
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

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.web.dto.CompanyDTO;
import com.screenleads.backend.app.web.dto.MediaSlimDTO;
import com.screenleads.backend.app.web.dto.MediaTypeDTO;

@Service
public class CompaniesServiceImpl implements CompaniesService {

    private final MediaTypeRepository mediaTypeRepository;
    private final CompanyRepository companyRepository;
    private final MediaRepository mediaRepository;
    private final AdviceRepository adviceRepository;
    private final DeviceRepository deviceRepository;

    public CompaniesServiceImpl(CompanyRepository companyRepository,
            MediaRepository mediaRepository,
            AdviceRepository adviceRepository,
            DeviceRepository deviceRepository,
            MediaTypeRepository mediaTypeRepository) {
        this.companyRepository = companyRepository;
        this.mediaRepository = mediaRepository;
        this.adviceRepository = adviceRepository;
        this.deviceRepository = deviceRepository;
        this.mediaTypeRepository = mediaTypeRepository;
    }

    // ===================== READ =====================

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDTO> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(CompanyDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyDTO> getCompanyById(Long id) {
        return companyRepository.findById(id).map(this::convertToDTO);
    }

    // ===================== WRITE =====================

    @Override
    @Transactional
    public CompanyDTO saveCompany(CompanyDTO companyDTO) {
        // Construir entidad base sin logo para evitar cascadas raras
        Company company = convertToEntity(companyDTO);
        company.setLogo(null);

        // Idempotencia por nombre
        Optional<Company> exist = companyRepository.findByName(company.getName());
        if (exist.isPresent()) {
            return convertToDTO(exist.get());
        }

        // Guardar primero la company para obtener ID
        Company savedCompany = companyRepository.save(company);

        // Logo: vincular existente o crear desde src
        if (companyDTO.logo() != null) {
            if (companyDTO.logo().id() != null) {
                Media media = mediaRepository.findById(companyDTO.logo().id())
                        .orElseThrow(
                                () -> new RuntimeException("Media no encontrado con id: " + companyDTO.logo().id()));
                if (media.getCompany() == null) {
                    media.setCompany(savedCompany);
                    mediaRepository.save(media);
                }
                savedCompany.setLogo(media);

            } else if (companyDTO.logo().src() != null && !companyDTO.logo().src().isBlank()) {
                Media newLogo = new Media();
                newLogo.setSrc(companyDTO.logo().src());
                newLogo.setCompany(savedCompany);
                setMediaTypeFromSrc(newLogo, companyDTO.logo().src());

                Media savedLogo = mediaRepository.save(newLogo);
                savedCompany.setLogo(savedLogo);

            } else {
                savedCompany.setLogo(null);
            }
        } else {
            savedCompany.setLogo(null);
        }

        savedCompany = companyRepository.save(savedCompany);
        return convertToDTO(savedCompany);
    }

    @Override
    @Transactional
    public CompanyDTO updateCompany(Long id, CompanyDTO companyDTO) {
        Company company = companyRepository.findById(id).orElseThrow();
        company.setName(companyDTO.name());
        company.setObservations(companyDTO.observations());
        company.setPrimaryColor(companyDTO.primaryColor());
        company.setSecondaryColor(companyDTO.secondaryColor());

        if (companyDTO.logo() != null) {
            if (companyDTO.logo().id() != null) {
                Media media = mediaRepository.findById(companyDTO.logo().id())
                        .orElseThrow(
                                () -> new RuntimeException("Media no encontrado con id: " + companyDTO.logo().id()));

                String newSrc = companyDTO.logo().src();
                if (newSrc != null && !newSrc.isBlank() && !java.util.Objects.equals(media.getSrc(), newSrc)) {
                    media.setSrc(newSrc);
                    setMediaTypeFromSrc(media, newSrc);
                }

                if (media.getCompany() == null) {
                    media.setCompany(company);
                }

                mediaRepository.save(media);
                company.setLogo(media);

            } else if (companyDTO.logo().src() != null && !companyDTO.logo().src().isBlank()) {
                Media newLogo = new Media();
                newLogo.setSrc(companyDTO.logo().src());
                newLogo.setCompany(company);
                setMediaTypeFromSrc(newLogo, companyDTO.logo().src());

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
    @Transactional
    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }

    // ===================== MAPPING =====================

    private CompanyDTO convertToDTO(Company company) {
        // Inicializa sólo lo necesario para evitar proxies LAZY en la serialización
        Media logo = company.getLogo();
        if (logo != null) {
            Hibernate.initialize(logo);
            MediaType type = logo.getType();
            if (type != null)
                Hibernate.initialize(type);
        }

        // Para no arrastrar colecciones LAZY (y evitar ByteBuddy proxies), devolvemos
        // vacías.
        // Si quieres devolverlas, mejor pásalas a DTOs o usa @EntityGraph y mapea.
        List<Device> devices = List.of();
        List<Advice> advices = List.of();

        return new CompanyDTO(
                company.getId(),
                company.getName(),
                company.getObservations(),
                toMediaSlimDTO(company.getLogo()),
                devices,
                advices,
                company.getPrimaryColor(),
                company.getSecondaryColor());
    }

    private MediaSlimDTO toMediaSlimDTO(Media m) {
        if (m == null)
            return null;
        return new MediaSlimDTO(
                m.getId(),
                m.getSrc(),
                toMediaTypeDTO(m.getType()),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }

    private MediaTypeDTO toMediaTypeDTO(MediaType t) {
        if (t == null)
            return null;
        return new MediaTypeDTO(
                t.getId(),
                t.getExtension(), // <-- correcto
                t.getType(), // <-- correcto
                t.getEnabled());
    }

    private Company convertToEntity(CompanyDTO dto) {
        Company c = new Company();
        c.setId(dto.id());
        c.setName(dto.name());
        c.setObservations(dto.observations());
        c.setPrimaryColor(dto.primaryColor());
        c.setSecondaryColor(dto.secondaryColor());

        // Logo: si viene id en el DTO slim, enlazamos; si no, se creará en save/update
        if (dto.logo() != null && dto.logo().id() != null) {
            mediaRepository.findById(dto.logo().id()).ifPresent(c::setLogo);
        } else {
            c.setLogo(null);
        }

        // No mapeamos devices/advices desde DTO para evitar inconsistencias (se
        // gestionan por sus endpoints)
        c.setDevices(List.of());
        c.setAdvices(List.of());

        return c;
    }

    // ===================== HELPERS =====================

    private void setMediaTypeFromSrc(Media media, String src) {
        if (src == null)
            return;
        String lower = src.toLowerCase();
        int dot = lower.lastIndexOf('.');
        if (dot != -1 && dot < lower.length() - 1) {
            String ext = lower.substring(dot + 1);
            mediaTypeRepository.findByExtension(ext).ifPresent(media::setType);
        }
    }

    private String deriveMediaName(String src, String prefix) {
        if (src == null || src.isBlank())
            return prefix + "-" + System.currentTimeMillis();
        String base = src;
        int q = base.indexOf('?');
        if (q >= 0)
            base = base.substring(0, q);
        int slash = base.lastIndexOf('/');
        if (slash >= 0 && slash < base.length() - 1)
            base = base.substring(slash + 1);
        if (base.isBlank())
            base = prefix + "-" + System.currentTimeMillis();
        return base;
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
        Integer width = dto.width() != null ? dto.width().intValue() : null;
        Integer height = dto.height() != null ? dto.height().intValue() : null;

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
        Integer width = deviceDTO.width() != null ? deviceDTO.width().intValue() : null;
        Integer height = deviceDTO.height() != null ? deviceDTO.height().intValue() : null;
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
        if (mediaDTO.type() == null || mediaDTO.type().getId() == null) {
            throw new IllegalArgumentException("Media type requerido");
        }
        media.setType(mediaTypeRepository.findById(mediaDTO.type().getId())
                .orElseThrow(() -> new IllegalArgumentException("Media type no encontrado")));
        // TODO: recibir o inferir companyId; sin eso, se caerá el save si es NOT NULL
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
// src/main/java/com/screenleads/backend/app/application/service/PermissionService.java
package com.screenleads.backend.app.application.service;

public interface PermissionService {
    boolean can(String resource, String action); // action: "create","read","update","delete"

    int effectiveLevel(); // nivel del rol del usuario actual
}

```

```java
// src/main/java/com/screenleads/backend/app/application/service/PermissionServiceImpl.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.AppEntityRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("perm") // 👈 clave: SpEL podrá resolver @perm
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final AppEntityRepository permissionRepository;
    private final UserRepository userRepository;

    @Override
    public boolean can(String resource, String action) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated())
                return false;

            User u = userRepository.findByUsername(auth.getName()).orElse(null);
            if (u == null || u.getRole() == null || u.getRole().getLevel() == null)
                return false;

            int myLevel = u.getRole().getLevel(); // 👈 rol único

            AppEntity p = permissionRepository.findByResource(resource).orElse(null);
            if (p == null)
                return false;

            Integer required = switch (action) {
                case "read" -> p.getReadLevel();
                case "create" -> p.getCreateLevel();
                case "update" -> p.getUpdateLevel();
                case "delete" -> p.getDeleteLevel();
                default -> null;
            };
            if (required == null)
                return false;

            // Niveles: 1 (más alto) permite todo ≤ requerido
            return myLevel <= required;
        } catch (Exception e) {
            log.warn("perm.can({}, {}) falló", resource, action, e);
            return false; // nunca romper la evaluación SpEL
        }
    }

    @Override
    public int effectiveLevel() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated())
                return Integer.MAX_VALUE;
            return userRepository.findByUsername(auth.getName())
                    .map(u -> (u.getRole() != null && u.getRole().getLevel() != null)
                            ? u.getRole().getLevel()
                            : Integer.MAX_VALUE)
                    .orElse(Integer.MAX_VALUE);
        } catch (Exception e) {
            log.warn("effectiveLevel() falló", e);
            return Integer.MAX_VALUE;
        }
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.application.service.PromotionService;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.domain.model.PromotionLead;
import com.screenleads.backend.app.domain.repositories.PromotionLeadRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;
import com.screenleads.backend.app.web.dto.LeadSummaryDTO;
import com.screenleads.backend.app.web.dto.PromotionDTO;
import com.screenleads.backend.app.web.dto.PromotionLeadDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionLeadRepository promotionLeadRepository;
    private final ObjectMapper objectMapper; // Autoconfigurado por Spring Boot

    // =========================================
    // CRUD Promotion
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll()
                .stream()
                .map(p -> map(p, PromotionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDTO getPromotionById(Long id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));
        return map(p, PromotionDTO.class);
    }

    @Override
    public PromotionDTO savePromotion(PromotionDTO dto) {
        // Mapear DTO -> Entity sin suponer getters concretos
        Promotion toSave = map(dto, Promotion.class);
        Promotion saved = promotionRepository.save(toSave);
        return map(saved, PromotionDTO.class);
    }

    @Override
    public PromotionDTO updatePromotion(Long id, PromotionDTO dto) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + id));

        // Creamos un "patch" a partir del DTO y fusionamos solo campos no nulos
        Promotion patch = map(dto, Promotion.class);
        mergeNonNull(patch, existing);

        // El entity está gestionado en la sesión; devolver mapeado a DTO
        return map(existing, PromotionDTO.class);
    }

    @Override
    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new IllegalArgumentException("Promotion not found: " + id);
        }
        promotionRepository.deleteById(id);
    }

    // =========================================
    // Leads
    // =========================================

    @Override
    public PromotionLeadDTO registerLead(Long promotionId, PromotionLeadDTO dto) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));

        // Mapear DTO -> Entity temporalmente para leer campos como identifier sin usar
        // getters del DTO
        PromotionLead candidate = map(dto, PromotionLead.class);

        // Si tienes unique (promotion_id + identifier), prevenimos duplicados
        if (candidate.getIdentifier() != null &&
                promotionLeadRepository.existsByPromotionIdAndIdentifier(promotionId, candidate.getIdentifier())) {
            throw new IllegalArgumentException("Lead already exists for identifier: " + candidate.getIdentifier());
        }

        candidate.setPromotion(promo);
        PromotionLead saved = promotionLeadRepository.save(candidate);
        return map(saved, PromotionLeadDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionLeadDTO> listLeads(Long promotionId) {
        return promotionLeadRepository.findByPromotionId(promotionId).stream()
                .map(l -> map(l, PromotionLeadDTO.class))
                .collect(Collectors.toList());
    }

    // =========================================
    // Export / Informes
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public String exportLeadsCsv(Long promotionId, ZonedDateTime from, ZonedDateTime to) {
        Instant fromI = from != null ? from.toInstant() : Instant.EPOCH;
        Instant toI = to != null ? to.toInstant() : Instant.now();

        List<PromotionLead> leads = promotionLeadRepository.findByPromotionId(promotionId).stream()
                .filter(l -> {
                    Instant c = l.getCreatedAt();
                    return (c.equals(fromI) || c.isAfter(fromI)) && (c.equals(toI) || c.isBefore(toI));
                })
                .sorted(Comparator.comparing(PromotionLead::getCreatedAt))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(
                "id,promotionId,identifierType,identifier,firstName,lastName,email,phone,birthDate,acceptedPrivacyAt,acceptedTermsAt,createdAt\n");
        for (PromotionLead l : leads) {
            sb.append(Optional.ofNullable(l.getId()).orElse(0L)).append(',')
                    .append(Optional.ofNullable(l.getPromotion()).map(Promotion::getId).orElse(null)).append(',')
                    .append(Optional.ofNullable(l.getIdentifierType()).map(Enum::name).orElse("")).append(',')
                    .append(csv(l.getIdentifier())).append(',')
                    .append(csv(l.getFirstName())).append(',')
                    .append(csv(l.getLastName())).append(',')
                    .append(csv(l.getEmail())).append(',')
                    .append(csv(l.getPhone())).append(',')
                    .append(Optional.ofNullable(l.getBirthDate()).orElse(null)).append(',')
                    .append(Optional.ofNullable(l.getAcceptedPrivacyAt()).orElse(null)).append(',')
                    .append(Optional.ofNullable(l.getAcceptedTermsAt()).orElse(null)).append(',')
                    .append(Optional.ofNullable(l.getCreatedAt()).orElse(null))
                    .append('\n');
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public LeadSummaryDTO getLeadSummary(Long promotionId, ZonedDateTime from, ZonedDateTime to) {
        ZoneId zone = ZoneId.systemDefault();
        Instant fromI = from != null ? from.toInstant() : Instant.EPOCH;
        Instant toI = to != null ? to.toInstant() : Instant.now();

        List<PromotionLead> leads = promotionLeadRepository.findByPromotionId(promotionId).stream()
                .filter(l -> {
                    Instant c = l.getCreatedAt();
                    return (c.equals(fromI) || c.isAfter(fromI)) && (c.equals(toI) || c.isBefore(toI));
                })
                .toList();

        long totalLeads = leads.size();
        long uniqueIdentifiers = leads.stream()
                .map(PromotionLead::getIdentifier)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Map<LocalDate, Long> leadsByDay = leads.stream().collect(
                java.util.stream.Collectors.groupingBy(
                        l -> l.getCreatedAt().atZone(zone).toLocalDate(),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.counting()));

        return new LeadSummaryDTO(promotionId, totalLeads, uniqueIdentifiers, leadsByDay);
    }

    @Override
    public PromotionLeadDTO createTestLead(Long promotionId, PromotionLeadDTO overrides) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));

        // Base "falsa" en Entity
        PromotionLead base = PromotionLead.builder()
                .identifierType(LeadIdentifierType.EMAIL)
                .identifier(UUID.randomUUID() + "@example.test")
                .firstName("Test")
                .lastName("Lead")
                .email(null) // lo puede aportar overrides
                .acceptedPrivacyAt(Instant.now())
                .acceptedTermsAt(Instant.now())
                .promotion(promo)
                .build();

        if (overrides != null) {
            PromotionLead patch = map(overrides, PromotionLead.class);
            mergeNonNull(patch, base);
        }

        PromotionLead saved = promotionLeadRepository.save(base);
        return map(saved, PromotionLeadDTO.class);
    }

    // =========================================
    // Helpers
    // =========================================

    private <T> T map(Object source, Class<T> targetType) {
        if (source == null)
            return null;
        return objectMapper.convertValue(source, targetType);
    }

    private static void mergeNonNull(Object src, Object target) {
        if (src == null || target == null)
            return;
        BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
    }

    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            String name = pd.getName();
            // ignorar "class"
            if ("class".equals(name))
                continue;
            Object value = src.getPropertyValue(name);
            if (value == null) {
                emptyNames.add(name);
            }
        }
        return emptyNames.toArray(new String[0]);
    }

    private static String csv(String s) {
        if (s == null)
            return "";
        String escaped = s.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
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
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.web.dto.UserDto;
import com.screenleads.backend.app.web.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final CompanyRepository companyRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService perm;
    private final MediaRepository mediaRepository;
    private final SecureRandom random = new SecureRandom();
    private final MediaTypeRepository mediaTypeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public UserServiceImpl(
            UserRepository repo,
            CompanyRepository companyRepo,
            RoleRepository roleRepo,
            PasswordEncoder passwordEncoder,
            PermissionService perm,
            MediaRepository mediaRepository,
            MediaTypeRepository mediaTypeRepository) {
        this.repo = repo;
        this.companyRepo = companyRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.perm = perm;
        this.mediaRepository = mediaRepository;
        this.mediaTypeRepository = mediaTypeRepository;
    }

    // ---------- LECTURAS (activamos filtro en la misma Session/Tx) ----------

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll() {
        enableCompanyFilterIfNeeded();
        return repo.findAll().stream()
                .peek(u -> {
                    if (u.getProfileImage() != null) Hibernate.initialize(u.getProfileImage());
                })
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        enableCompanyFilterIfNeeded();
        return repo.findById(id)
                .map(u -> {
                    if (u.getProfileImage() != null) Hibernate.initialize(u.getProfileImage());
                    return UserMapper.toDto(u);
                })
                .orElse(null);
    }

    // ---------------------------- ESCRITURAS ----------------------------

    @Override
    @Transactional
    public void delete(Long id) {
        if (!perm.can("user", "delete")) {
            throw new IllegalArgumentException("No autorizado a borrar usuarios");
        }
        enableCompanyFilterIfNeeded(); // protege que no borre fuera de su compañía si no es admin
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public UserDto create(UserDto dto) {
        if (dto == null)
            throw new IllegalArgumentException("Body requerido");
        if (dto.getUsername() == null || dto.getUsername().isBlank())
            throw new IllegalArgumentException("username requerido");

        // Permisos para crear y jerarquía de nivel
        assertCanCreateUser();

        repo.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("username ya existe");
        });

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setName(dto.getName());
        u.setLastName(dto.getLastName());

        // Password
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
            Long currentCompanyId = currentCompanyId();
            if (currentCompanyId != null && !isCurrentUserAdmin()) {
                Company c = companyRepo.findById(currentCompanyId)
                        .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + currentCompanyId));
                u.setCompany(c);
            }
        }

        // Rol ÚNICO desde el DTO
        Role role = resolveRoleFromDto(dto);
        if (role == null)
            throw new IllegalArgumentException("Se requiere un rol");

        // Verificación de jerarquía (nivel)
        assertAssignableRole(role);

        // Asignar como set (si la entidad User mantiene colección)
        u.setRole(role);

        User saved = repo.save(u);
        return UserMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto dto) {
        enableCompanyFilterIfNeeded();

        return repo.findById(id).map(existing -> {
            if (dto.getUsername() != null)
                existing.setUsername(dto.getUsername());
            if (dto.getEmail() != null)
                existing.setEmail(dto.getEmail());
            if (dto.getName() != null)
                existing.setName(dto.getName());
            if (dto.getLastName() != null)
                existing.setLastName(dto.getLastName());

            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            // --- ACTUALIZAR COMPAÑÍA ---
            Long finalCompanyId;
            if (dto.getCompanyId() != null) {
                finalCompanyId = dto.getCompanyId();
            } else if (dto.getCompany() != null && dto.getCompany().id() != null) {
                finalCompanyId = dto.getCompany().id();
            } else {
                finalCompanyId = null;
            }
            if (finalCompanyId != null) {
                if (!isCurrentUserAdmin()) {
                    Long currentCompanyId = currentCompanyId();
                    if (currentCompanyId == null || !currentCompanyId.equals(finalCompanyId)) {
                        throw new IllegalArgumentException("No autorizado a cambiar de compañía");
                    }
                }
                Company c = companyRepo.findById(finalCompanyId)
                        .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + finalCompanyId));
                existing.setCompany(c);
            }

            // --- ACTUALIZAR IMAGEN DE PERFIL ---
            if (dto.getProfileImage() != null && dto.getProfileImage().src() != null) {
                var mediaOpt = mediaRepository.findBySrc(dto.getProfileImage().src());
                if (mediaOpt.isPresent()) {
                    existing.setProfileImage(mediaOpt.get());
                } else {
                    // Crear Media nueva
                    var mediaDto = dto.getProfileImage();
                    // Buscar o crear MediaType
                    var typeDto = mediaDto.type();
                    MediaType type = null;
                    String src = mediaDto.src();
                    final String extension;
                    if (src != null && src.contains(".")) {
                        extension = src.substring(src.lastIndexOf('.') + 1).toLowerCase();
                    } else {
                        extension = null;
                    }
                    String detectedType = null;
                    if (extension != null) {
                        switch (extension) {
                            case "jpg": case "jpeg": case "png": case "gif": case "bmp":
                                detectedType = "IMG"; break;
                            case "mp4": case "avi": case "mov": case "wmv":
                                detectedType = "VIDEO"; break;
                            case "mp3": case "wav": case "ogg":
                                detectedType = "AUDIO"; break;
                            default:
                                detectedType = "FILE"; break;
                        }
                    }
                    if (typeDto != null) {
                        if (typeDto.type() != null && !typeDto.type().isBlank()) {
                            type = mediaTypeRepository.findByType(typeDto.type()).orElse(null);
                        }
                        if (type == null && typeDto.extension() != null && !typeDto.extension().isBlank()) {
                            type = mediaTypeRepository.findByExtension(typeDto.extension()).orElse(null);
                        }
                    }
                    if (type == null && detectedType != null && extension != null) {
                        type = mediaTypeRepository.findByType(detectedType)
                                .filter(t -> t.getExtension().equalsIgnoreCase(extension))
                                .orElse(null);
                        if (type == null) {
                            type = new MediaType();
                            type.setType(detectedType);
                            type.setExtension(extension);
                            type.setEnabled(true);
                            type = mediaTypeRepository.save(type);
                        }
                    }
                    if (type == null) {
                        // fallback seguro
                        type = mediaTypeRepository.findByType("IMG").orElseGet(() -> {
                            MediaType t = new MediaType();
                            t.setType("IMG");
                            t.setExtension("jpg");
                            t.setEnabled(true);
                            return mediaTypeRepository.save(t);
                        });
                    }
                    // Buscar compañía
                    Company company = existing.getCompany();
                    if (company == null && dto.getCompanyId() != null) {
                        company = companyRepo.findById(dto.getCompanyId()).orElse(null);
                    }
                    if (company == null && dto.getCompany() != null && dto.getCompany().id() != null) {
                        company = companyRepo.findById(dto.getCompany().id()).orElse(null);
                    }
                    if (company == null) {
                        throw new IllegalArgumentException("No se puede asociar media: compañía no encontrada");
                    }
            var newMedia = Media.builder()
                .src(mediaDto.src())
                .type(type)
                .company(company)
                .build();
            existing.setProfileImage(mediaRepository.save(newMedia));
                }
            }

            // Si llega un nombre de rol en el DTO, cambiarlo con mismas reglas
            if (dto.getRole() != null) {
                if (!perm.can("user", "update"))
                    throw new IllegalArgumentException("No autorizado a actualizar usuarios");
                Role newRole = resolveRoleFromDto(dto);
                if (newRole == null)
                    throw new IllegalArgumentException("Rol inválido");
                assertAssignableRole(newRole);
                existing.setRole(newRole);
            }

            User saved = repo.save(existing);
            return UserMapper.toDto(saved);
        }).orElse(null);
    }

    // ---------------------------- HELPERS ----------------------------

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

        if (principal instanceof User u) {
            return (u.getCompany() != null) ? u.getCompany().getId() : null;
        }
        if (principal instanceof UserDetails ud) {
            return repo.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }
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

    // ======== PERMISOS / NIVELES ========

    private int currentEffectiveLevel() {
        return perm.effectiveLevel();
    }

    private void assertCanCreateUser() {
        if (!perm.can("user", "create"))
            throw new IllegalArgumentException("No autorizado a crear usuarios");
        int L = currentEffectiveLevel();
        if (L > 2) // sólo niveles 1 o 2 pueden crear
            throw new IllegalArgumentException("Solo roles de nivel 1 o 2 pueden crear usuarios");
    }

    private void assertAssignableRole(Role targetRole) {
        int myLevel = currentEffectiveLevel();
        Integer roleLevel = Optional.ofNullable(targetRole.getLevel())
                .orElse(Integer.MAX_VALUE);
        if (roleLevel < myLevel) {
            // ej.: soy nivel 2, intento asignar nivel 1 → prohibido
            throw new IllegalArgumentException("No puedes asignar un rol superior al tuyo");
        }
    }

    /**
     * Resuelve un rol único desde el DTO:
     * - Si llega roleId → busca por id.
     * - Si llega role (nombre/código) → busca por nombre.
     */
    private Role resolveRoleFromDto(UserDto dto) {
        if (dto.getRole() == null)
            return null;
        if (dto.getRole().id() != null) {
            return roleRepo.findById(dto.getRole().id())
                    .orElseThrow(() -> new IllegalArgumentException("roleId inválido: " + dto.getRole().id()));
        }
        if (dto.getRole().role() != null) {
            return roleRepo.findByRole(dto.getRole().role())
                    .orElseThrow(() -> new IllegalArgumentException("role inválido: " + dto.getRole().role()));
        }
        return null;
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

