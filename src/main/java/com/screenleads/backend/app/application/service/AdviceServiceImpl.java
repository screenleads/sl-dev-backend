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

    private static final String ADVICE_NOT_FOUND = "Advice not found: ";

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

            // Skip si no está en rango de fechas o si no tiene horarios
            if (!dateOk || s.getWindows() == null || s.getWindows().isEmpty())
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
        advice.setMedia(resolveMediaForCreate(dto.getMedia(), advice.getCompany()));
        advice.setPromotion(resolvePromotionFromDto(dto.getPromotion()));

        advice.setSchedules(buildSchedulesForAdvice(dto.getSchedules(), advice));

        validateAdvice(advice);
        Advice saved = adviceRepository.save(advice);
        logSavedAdvice(saved);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public AdviceDTO updateAdvice(Long id, AdviceDTO dto) {
        enableCompanyFilterIfNeeded();

        Advice advice = adviceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(ADVICE_NOT_FOUND + id));

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

        List<AdviceTimeWindow> windows = convertTimeWindows(sDto);
        validateAndNormalizeWindows(windows);

        for (AdviceTimeWindow w : windows) {
            w.setSchedule(s);
        }
        s.setWindows(windows);
        return s;
    }

    private List<AdviceTimeWindow> convertTimeWindows(AdviceScheduleDTO sDto) {
        List<AdviceTimeWindow> windows = new ArrayList<>();

        // Soportar ambos formatos: windows plano y dayWindows agrupado
        if (sDto.getWindows() != null && !sDto.getWindows().isEmpty()) {
            windows.addAll(convertFromPlainWindows(sDto.getWindows()));
        } else if (sDto.getDayWindows() != null && !sDto.getDayWindows().isEmpty()) {
            windows.addAll(convertFromDayWindows(sDto.getDayWindows()));
        }

        return windows;
    }

    private List<AdviceTimeWindow> convertFromPlainWindows(List<AdviceTimeWindowDTO> windowDtos) {
        List<AdviceTimeWindow> windows = new ArrayList<>();
        for (AdviceTimeWindowDTO wDto : windowDtos) {
            AdviceTimeWindow w = new AdviceTimeWindow();
            w.setWeekday(parseWeekday(wDto.getWeekday()));
            w.setFromTime(parseTime(wDto.getFromTime()));
            w.setToTime(parseTime(wDto.getToTime()));
            windows.add(w);
        }
        return windows;
    }

    private List<AdviceTimeWindow> convertFromDayWindows(List<AdviceScheduleDTO.DayWindowDTO> dayWindows) {
        List<AdviceTimeWindow> windows = new ArrayList<>();
        for (AdviceScheduleDTO.DayWindowDTO day : dayWindows) {
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
        return windows;
    }

    private AdviceDTO convertToDTO(Advice advice) {
        MediaUpsertDTO mediaDto = buildMediaRef(advice.getMedia());
        PromotionRefDTO promoDto = buildPromotionRef(advice.getPromotion());
        CompanyRefDTO companyDto = buildCompanyRef(advice.getCompany());
        Number intervalValue = (advice.getInterval() == null) ? null : advice.getInterval().getSeconds();
        List<AdviceScheduleDTO> schedules = buildScheduleDTOs(advice.getSchedules());

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

    private MediaUpsertDTO buildMediaRef(Media media) {
        return (media != null) ? new MediaUpsertDTO(media.getId(), media.getSrc()) : null;
    }

    private PromotionRefDTO buildPromotionRef(Promotion promotion) {
        return (promotion != null) ? new PromotionRefDTO(promotion.getId()) : null;
    }

    private CompanyRefDTO buildCompanyRef(Company company) {
        return (company != null) ? new CompanyRefDTO(company.getId(), company.getName()) : null;
    }

    private List<AdviceScheduleDTO> buildScheduleDTOs(List<AdviceSchedule> schedules) {
        List<AdviceScheduleDTO> scheduleDtos = new ArrayList<>();
        if (schedules != null) {
            for (AdviceSchedule s : schedules) {
                List<AdviceTimeWindowDTO> wins = buildTimeWindowDTOs(s.getWindows());
                scheduleDtos.add(new AdviceScheduleDTO(
                        s.getId(),
                        formatDate(s.getStartDate()),
                        formatDate(s.getEndDate()),
                        wins,
                        null));
            }
        }
        return scheduleDtos;
    }

    private List<AdviceTimeWindowDTO> buildTimeWindowDTOs(List<AdviceTimeWindow> windows) {
        List<AdviceTimeWindowDTO> wins = new ArrayList<>();
        if (windows != null) {
            for (AdviceTimeWindow w : windows) {
                wins.add(new AdviceTimeWindowDTO(
                        w.getId(),
                        w.getWeekday() != null ? w.getWeekday().name() : null,
                        formatTime(w.getFromTime()),
                        formatTime(w.getToTime())));
            }
        }
        return wins;
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
    // ============================= MEDIA RESOLUTION =============================

    private Media resolveMediaForCreate(MediaUpsertDTO mediaDto, Company company) {
        if (mediaDto == null) {
            return null;
        }

        if (mediaDto.id() != null && mediaDto.id() > 0) {
            return mediaRepository.findById(mediaDto.id()).orElse(null);
        }

        if (mediaDto.src() != null && !mediaDto.src().isBlank()) {
            String src = mediaDto.src().trim();
            return mediaRepository.findBySrc(src)
                    .orElseGet(() -> createNewMedia(src, company));
        }

        return null;
    }

    private Media createNewMedia(String src, Company company) {
        MediaType mediaType = resolveMediaType(src);
        if (company == null || mediaType == null) {
            return null;
        }

        Media newMedia = new Media();
        newMedia.setSrc(src);
        newMedia.setType(mediaType);
        newMedia.setCompany(company);
        return mediaRepository.save(newMedia);
    }

    private MediaType resolveMediaType(String src) {
        String ext = extractExtension(src);
        if (ext != null) {
            return mediaTypeRepository.findByExtension(ext).orElse(null);
        }
        return mediaTypeRepository.findAll().stream().findFirst().orElse(null);
    }

    private String extractExtension(String src) {
        int dotIdx = src.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < src.length() - 1) {
            return src.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
        }
        return null;
    }

    // ============================= SCHEDULE BUILDING =============================

    private List<AdviceSchedule> buildSchedulesForAdvice(List<AdviceScheduleDTO> scheduleDtos, Advice advice) {
        List<AdviceSchedule> schedules = new ArrayList<>();
        if (scheduleDtos != null) {
            for (AdviceScheduleDTO sDto : scheduleDtos) {
                AdviceSchedule mappedSchedule = mapScheduleDTO(sDto, advice);
                logScheduleMapping(mappedSchedule);
                schedules.add(mappedSchedule);
            }
        }
        return schedules;
    }

    private void logScheduleMapping(AdviceSchedule schedule) {
        log.debug("[AdviceServiceImpl] Schedule mapeado: startDate={}, endDate={}",
                schedule.getStartDate(), schedule.getEndDate());
        if (schedule.getWindows() != null) {
            for (AdviceTimeWindow win : schedule.getWindows()) {
                log.debug("[AdviceServiceImpl] Window mapeado: weekday={}, from={}, to={}",
                        win.getWeekday(), win.getFromTime(), win.getToTime());
            }
        }
    }

    private void logSavedAdvice(Advice saved) {
        int totalWindows = 0;
        if (saved.getSchedules() != null) {
            totalWindows = saved.getSchedules().stream()
                    .mapToInt(s -> s.getWindows() == null ? 0 : s.getWindows().size())
                    .sum();
        }
        log.debug("[AdviceServiceImpl] Advice guardado con ID {}. Total ventanas: {}", saved.getId(), totalWindows);
    }

    // ============================= COMPANY FILTER =============================

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
