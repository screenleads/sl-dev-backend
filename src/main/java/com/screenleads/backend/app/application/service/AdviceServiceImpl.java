package com.screenleads.backend.app.application.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.TimeRange;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;

@Service
public class AdviceServiceImpl implements AdviceService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

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

    // ======================= LECTURAS (filtradas por compañía)
    // =======================

    @Override
    @Transactional
    public List<AdviceDTO> getAllAdvices() {
        enableCompanyFilterIfNeeded();
        return adviceRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(AdviceDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AdviceDTO> getVisibleAdvicesNow() {
        enableCompanyFilterIfNeeded();

        LocalDateTime now = LocalDateTime.now();
        DayOfWeek today = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        return adviceRepository.findAll().stream()
                .filter(advice -> advice.getVisibilityRules() != null &&
                        advice.getVisibilityRules().stream()
                                .anyMatch(rule -> rule.getDay() == today &&
                                        rule.getTimeRanges() != null &&
                                        rule.getTimeRanges().stream()
                                                .anyMatch(range -> !time.isBefore(range.getFromTime()) &&
                                                        !time.isAfter(range.getToTime()))))
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(AdviceDTO::id))
                .collect(Collectors.toList());
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
        enableCompanyFilterIfNeeded(); // por si hay lecturas internas

        // 1) Crear Advice "limpio"
        Advice advice = new Advice();
        advice.setDescription(dto.description());
        advice.setCustomInterval(dto.customInterval());
        advice.setInterval(dto.interval());

        // 2) Resolver Media (si viene id)
        if (dto.media() != null && dto.media().getId() != null) {
            Media media = mediaRepository.findById(dto.media().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Media no encontrada"));
            advice.setMedia(media);
        } else {
            advice.setMedia(null);
        }

        // 3) Resolver Promotion (según tu diseño actual)
        advice.setPromotion(dto.promotion());

        // 4) (Multi-tenant) Fijar compañía si no es admin
        enforceCompanyOnWrite(advice);

        // 5) Construir visibilityRules + timeRanges
        advice.setVisibilityRules(new ArrayList<>());
        if (dto.visibilityRules() != null) {
            for (AdviceVisibilityRule ruleIn : dto.visibilityRules()) { // ENTIDAD (tu DTO usa entidades)
                AdviceVisibilityRule rule = new AdviceVisibilityRule();
                rule.setDay(ruleIn.getDay());
                rule.setAdvice(advice);

                List<TimeRange> ranges = new ArrayList<>();
                if (ruleIn.getTimeRanges() != null) {
                    for (TimeRange rangeIn : ruleIn.getTimeRanges()) { // ENTIDAD
                        TimeRange tr = new TimeRange();
                        tr.setFromTime(rangeIn.getFromTime());
                        tr.setToTime(rangeIn.getToTime());
                        tr.setRule(rule);
                        ranges.add(tr);
                    }
                }
                rule.setTimeRanges(ranges);
                advice.getVisibilityRules().add(rule);
            }
        }

        // 6) Guardar
        Advice saved = adviceRepository.save(advice);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public AdviceDTO updateAdvice(Long id, AdviceDTO dto) {
        enableCompanyFilterIfNeeded(); // findById ya vendrá filtrado por compañía

        Advice advice = adviceRepository.findById(id).orElseThrow();

        advice.setCustomInterval(dto.customInterval());
        advice.setDescription(dto.description());
        advice.setInterval(dto.interval());

        if (dto.media() != null && dto.media().getId() != null) {
            Media media = mediaRepository.findById(dto.media().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Media no encontrada"));
            advice.setMedia(media);
        } else {
            advice.setMedia(null);
        }

        advice.setPromotion(dto.promotion());

        // Reemplazo completo de reglas (orphanRemoval aplicado)
        advice.getVisibilityRules().clear();

        if (dto.visibilityRules() != null) {
            for (AdviceVisibilityRule ruleDto : dto.visibilityRules()) {
                AdviceVisibilityRule rule = new AdviceVisibilityRule();
                rule.setDay(ruleDto.getDay());
                rule.setAdvice(advice);

                List<TimeRange> ranges = new ArrayList<>();
                if (ruleDto.getTimeRanges() != null) {
                    for (TimeRange rangeDto : ruleDto.getTimeRanges()) {
                        TimeRange tr = new TimeRange();
                        tr.setFromTime(rangeDto.getFromTime());
                        tr.setToTime(rangeDto.getToTime());
                        tr.setRule(rule);
                        ranges.add(tr);
                    }
                }
                rule.setTimeRanges(ranges);

                // Añade a la colección MANAGED del padre
                advice.getVisibilityRules().add(rule);
            }
        }

        // (Multi-tenant) Por si llega un company ajeno en el body
        enforceCompanyOnWrite(advice);

        Advice saved = adviceRepository.save(advice);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteAdvice(Long id) {
        enableCompanyFilterIfNeeded();
        adviceRepository.findById(id).ifPresent(adviceRepository::delete);
    }

    // ============================= MAPPERS =============================

    // Convert Advice Entity to AdviceDTO
    private AdviceDTO convertToDTO(Advice advice) {
        Media media = null;
        if (advice.getMedia() != null && advice.getMedia().getId() != null) {
            media = mediaRepository.findById(advice.getMedia().getId()).orElse(null);
        }
        return new AdviceDTO(
                advice.getId(),
                advice.getDescription(),
                advice.getCustomInterval(),
                advice.getInterval(),
                media,
                advice.getPromotion(),
                advice.getVisibilityRules());
    }

    // ============================= HELPERS MT =============================

    /**
     * Activa el filtro "companyFilter" en la misma Session/Tx usada por los repos
     * si NO es admin.
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

    /** Si el usuario NO es admin, fuerza que el Advice pertenezca a su compañía. */
    private void enforceCompanyOnWrite(Advice advice) {
        if (advice == null || isCurrentUserAdmin())
            return;
        Long companyId = currentCompanyId();
        if (companyId == null)
            return;
        if (advice.getCompany() == null || advice.getCompany().getId() == null
                || !companyId.equals(advice.getCompany().getId())) {
            Company c = new Company();
            c.setId(companyId);
            advice.setCompany(c);
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
        if (principal instanceof com.screenleads.backend.app.domain.model.User u) {
            return (u.getCompany() != null) ? u.getCompany().getId() : null;
        }

        // 2) UserDetails estándar
        if (principal instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        // 3) Principal como String (username) - típico JWT con "sub"
        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        return null;
    }
}
