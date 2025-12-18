package com.screenleads.backend.app.application.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
                .toList();
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
                .toList();
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

        log.debug("[AdviceServiceImpl] Recibido AdviceDTO: {}", dto);

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
                log.debug("[AdviceServiceImpl] Schedule mapeado: startDate={}, endDate={}", 
                        mappedSchedule.getStartDate(), mappedSchedule.getEndDate());
                if (mappedSchedule.getWindows() != null) {
                    for (AdviceTimeWindow win : mappedSchedule.getWindows()) {
                        log.debug("[AdviceServiceImpl] Window mapeado: weekday={}, from={}, to={}",
                                win.getWeekday(), win.getFromTime(), win.getToTime());
                    }
                }
                advice.getSchedules().add(mappedSchedule);
            }
        }

        validateAdvice(advice);
        Advice saved = adviceRepository.save(advice);
        // Log de persistencia real de ventanas
        int totalWindows = saved.getSchedules() == null ? 0
                : saved.getSchedules().stream().mapToInt(s -> s.getWindows() == null ? 0 : s.getWindows().size()).sum();
        log.debug("[AdviceServiceImpl] Advice guardado con ID {}. Total ventanas: {}", saved.getId(), totalWindows);
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
                                formatTime(w.getToTime())));
                    }
                }
                schedules.add(new AdviceScheduleDTO(
                        s.getId(),
                        formatDate(s.getStartDate()),
                        formatDate(s.getEndDate()),
                        wins,
                        null));
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
                .anyMatch(Set.of("ROLE_ADMIN", "ADMIN")::contains);
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
                .anyMatch(Set.of("ROLE_ADMIN", "ADMIN")::contains);
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
