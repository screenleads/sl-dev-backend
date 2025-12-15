# Seguridad — snapshot incrustado

> Config y clases de seguridad (Spring Security 6).

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/application/security/ApiKeyAuthenticationFilter.java
package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import com.screenleads.backend.app.domain.model.Client;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final ClientRepository clientRepository;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository, ClientRepository clientRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // ✅ Si ya hay autenticación (por ejemplo JWT), no machacamos
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-KEY");
        String clientId = request.getHeader("client_id");
        if (clientId == null || clientId.isEmpty()) {
            clientId = request.getHeader("client-id");
        }

        log.info("🔐 ApiKeyAuthenticationFilter - Path: {}, API-KEY: {}, client-id: {}",
                request.getRequestURI(),
                apiKey != null ? "***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) : "null",
                clientId);

        if (apiKey != null && clientId != null) {
            Optional<Client> clientOpt = clientRepository.findByClientIdAndActiveTrue(clientId);
            if (clientOpt.isPresent()) {
                Client client = clientOpt.get();
                log.info("✅ Cliente encontrado: ID={}, clientId={}", client.getId(), client.getClientId());

                Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyAndClientAndActiveTrue(apiKey, client);
                if (keyOpt.isPresent()) {
                    ApiKey key = keyOpt.get();
                    // Usar el ID de la API key como principal para identificar exactamente qué API
                    // key se está usando
                    Long apiKeyId = key.getId();
                    log.info("✅ API Key válida encontrada - ID: {}, ClientDbId: {}, Permissions: {}",
                            apiKeyId, client.getId(), key.getPermissions());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            apiKeyId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("API_CLIENT")));
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("✅ Autenticación establecida para apiKeyId: {}", apiKeyId);
                } else {
                    log.warn("❌ API Key no encontrada o inactiva");
                }
            } else {
                log.warn("❌ Cliente no encontrado o inactivo con clientId: {}", clientId);
            }
        }

        filterChain.doFilter(request, response);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/ApiKeyCompanyFilterEnabler.java
package com.screenleads.backend.app.application.security;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtro que habilita automáticamente el filtro de Hibernate para restringir
 * las consultas a una compañía específica cuando una API Key con alcance
 * restringido está autenticada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyCompanyFilterEnabler extends OncePerRequestFilter {

    private final EntityManager entityManager;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.isAuthenticated()) {
                // Verificar si es una autenticación de API_CLIENT
                boolean isApiClient = auth.getAuthorities().stream()
                    .anyMatch(a -> "API_CLIENT".equals(a.getAuthority()));

                if (isApiClient) {
                    Object principal = auth.getPrincipal();
                    
                    if (principal instanceof ApiKeyPrincipal) {
                        ApiKeyPrincipal apiKeyPrincipal = (ApiKeyPrincipal) principal;
                        
                        // Si tiene alcance restringido a una compañía, habilitar el filtro
                        if (apiKeyPrincipal.hasRestrictedAccess()) {
                            Long companyId = apiKeyPrincipal.getCompanyScope();
                            enableCompanyFilter(companyId);
                            log.debug("Filtro de compañía habilitado para API Key. CompanyId: {}", companyId);
                        } else {
                            log.debug("API Key con acceso global. No se aplica filtro de compañía.");
                        }
                    }
                }
            }

            filterChain.doFilter(request, response);
            
        } finally {
            // Limpiar filtros de Hibernate al terminar la petición
            disableCompanyFilter();
        }
    }

    /**
     * Habilita el filtro de Hibernate para la compañía específica.
     */
    private void enableCompanyFilter(Long companyId) {
        try {
            Session session = entityManager.unwrap(Session.class);
            org.hibernate.Filter filter = session.enableFilter("companyFilter");
            filter.setParameter("companyId", companyId);
            log.trace("Filtro Hibernate 'companyFilter' habilitado con companyId={}", companyId);
        } catch (Exception e) {
            log.warn("No se pudo habilitar el filtro de compañía", e);
        }
    }

    /**
     * Deshabilita el filtro de Hibernate.
     */
    private void disableCompanyFilter() {
        try {
            Session session = entityManager.unwrap(Session.class);
            session.disableFilter("companyFilter");
            log.trace("Filtro Hibernate 'companyFilter' deshabilitado");
        } catch (Exception e) {
            log.trace("Error deshabilitando filtro de compañía (puede ser normal si no estaba habilitado)", e);
        }
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/ApiKeyPrincipal.java
package com.screenleads.backend.app.application.security;

import java.util.Set;

/**
 * Representa el principal de autenticación para una API Key.
 * Contiene toda la información necesaria para verificar permisos y aplicar filtros de datos.
 */
public class ApiKeyPrincipal {
    private final Long apiKeyId;
    private final String clientId;
    private final Long clientDbId;
    private final Set<String> permissions;
    private final Long companyScope; // null = acceso a todas las compañías

    public ApiKeyPrincipal(Long apiKeyId, String clientId, Long clientDbId, Set<String> permissions, Long companyScope) {
        this.apiKeyId = apiKeyId;
        this.clientId = clientId;
        this.clientDbId = clientDbId;
        this.permissions = permissions;
        this.companyScope = companyScope;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public String getClientId() {
        return clientId;
    }

    public Long getClientDbId() {
        return clientDbId;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public Long getCompanyScope() {
        return companyScope;
    }

    /**
     * Verifica si tiene un permiso específico.
     * @param resource El recurso (ej: "snapshot", "lead", "company")
     * @param action La acción (ej: "read", "create", "update", "delete")
     * @return true si tiene el permiso
     */
    public boolean hasPermission(String resource, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        // Formatos soportados:
        // - "snapshot:read" (permiso específico)
        // - "snapshot:*" (todas las acciones sobre snapshot)
        // - "*:read" (leer cualquier recurso)
        // - "*:*" o "*" (superadmin)
        
        String specific = resource + ":" + action;
        String allActions = resource + ":*";
        String allResources = "*:" + action;
        
        return permissions.contains(specific) 
            || permissions.contains(allActions)
            || permissions.contains(allResources)
            || permissions.contains("*:*")
            || permissions.contains("*");
    }

    /**
     * Indica si tiene acceso global a todas las compañías.
     * @return true si puede acceder a datos de todas las compañías sin filtro
     */
    public boolean hasGlobalAccess() {
        return companyScope == null;
    }

    /**
     * Indica si está restringido a una compañía específica.
     * @return true si solo puede acceder a datos de una compañía
     */
    public boolean hasRestrictedAccess() {
        return companyScope != null;
    }

    @Override
    public String toString() {
        return "ApiKeyPrincipal{" +
                "apiKeyId=" + apiKeyId +
                ", clientId='" + clientId + '\'' +
                ", clientDbId=" + clientDbId +
                ", permissions=" + permissions +
                ", companyScope=" + companyScope +
                '}';
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/AuthenticationService.java
package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.application.security.jwt.JwtService;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.*;
import com.screenleads.backend.app.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public JwtResponse register(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setLastName(request.getLastName());

        if (request.getCompanyId() != null) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));
            user.setCompany(company);
        }

        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByRole("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Role ROLE_ADMIN not found"));
            user.setRole(adminRole);
        } else {
            Role defaultRole = roleRepository.findByRole("ROLE_COMPANY_VIEWER")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setRole(defaultRole);
        }

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return JwtResponse.builder()
                .accessToken(token)
                .user(UserMapper.toDto(user)) // <<< DTO, no entidad
                .build();
    }

    public JwtResponse login(LoginRequest request) throws AuthenticationException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtService.generateToken(user);
        return JwtResponse.builder()
                .accessToken(token)
                .user(UserMapper.toDto(user)) // <<< DTO, no entidad
                .build();
    }

    @Transactional
    public UserDto getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UsernameNotFoundException("No authenticated user");
        }
        User u;
        if (auth.getPrincipal() instanceof User) {
            User userPrincipal = (User) auth.getPrincipal();
            // Always reload from DB to ensure eager fetch
            u = userRepository.findWithCompanyAndProfileImageByUsername(userPrincipal.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        } else {
            String username = auth.getName();
            u = userRepository.findWithCompanyAndProfileImageByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        }
        return UserMapper.toDto(u);
    }

    public JwtResponse refreshToken() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UsernameNotFoundException("No authenticated user");
        }
        User user;
        if (auth.getPrincipal() instanceof User) {
            user = (User) auth.getPrincipal();
        } else {
            String username = auth.getName();
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        }
        String token = jwtService.generateToken(user);
        return JwtResponse.builder()
                .accessToken(token)
                .user(UserMapper.toDto(user))
                .build();
    }

    public void changePassword(PasswordChangeRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/CustomAccessDeniedHandler.java
package com.screenleads.backend.app.application.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements org.springframework.security.web.access.AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        res.setContentType("application/json;charset=UTF-8");
        String body = String.format("{\"error\":\"Forbidden\",\"message\":\"Insufficient permissions\",\"path\":\"%s\"}", 
                req.getRequestURI());
        res.getWriter().write(body);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/CustomAuthenticationEntryPoint.java
package com.screenleads.backend.app.application.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        res.setContentType("application/json;charset=UTF-8");
        String body = String.format("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing token\",\"path\":\"%s\"}", 
                req.getRequestURI());
        res.getWriter().write(body);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/SecurityConfig.java
package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.application.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final ApiKeyAuthenticationFilter apiKeyAuthFilter;
        private final UserDetailsService userDetailsService;

        // Respuestas JSON limpias para 401 / 403
        private final CustomAuthenticationEntryPoint authEntryPoint;
        private final CustomAccessDeniedHandler accessDeniedHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(eh -> eh
                                                .authenticationEntryPoint(authEntryPoint) // 401
                                                .accessDeniedHandler(accessDeniedHandler) // 403
                                )
                                .authorizeHttpRequests(auth -> auth
                                                // Preflight
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // WebSocket público (handshake/info) y status
                                                .requestMatchers(HttpMethod.GET, "/chat-socket/**").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/chat-socket/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/ws/status").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/ws/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/ws/command/**").authenticated()

                                                .requestMatchers(
                                                                com.screenleads.backend.app.infraestructure.config.SwaggerWhitelist.ENDPOINTS)
                                                .permitAll()

                                                // Auth: solo login/refresh públicos
                                                .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                                                // /auth/me requiere autenticación
                                                .requestMatchers("/auth/me").authenticated()

                                                // Swagger / OpenAPI / Health
                                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers("/actuator/health").permitAll()

                                                // El resto autenticado
                                                .anyRequest().authenticated())
                                .authenticationProvider(authenticationProvider())

                                // ✅ IMPORTANTE: anclar SIEMPRE a un filtro estándar (no a filtros custom)
                                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * CORS global: permite orígenes y *headers* usados por tu interceptor.
         * Importante: como allowCredentials=true, no se puede usar "*" en
         * allowedOrigins.
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                // Orígenes explícitos (ajusta según tus entornos)
                config.setAllowedOrigins(List.of(
                                "https://localhost",
                                "https://localhost:4200",
                                "https://localhost:8100",
                                "http://localhost:4200",
                                "http://localhost:8100",
                                "https://sl-device-connector.web.app",
                                "https://sl-dev-dashboard-pre-c4a3c4b00c91.herokuapp.com",
                                "https://sl-dev-dashboard-bfb13611d5d6.herokuapp.com"
                // añade aquí preprod/prod si procede
                ));

                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

                // Headers que envía el interceptor
                config.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "Origin",
                                "X-Requested-With",
                                "X-Timezone",
                                "X-Timezone-Offset",
                                "Accept-Language",
                                "X-API-KEY",
                                "client_id",
                                "client-id"));

                // (Opcional) Headers expuestos al frontend si necesitas leerlos
                config.setExposedHeaders(List.of(
                                "Authorization",
                                "X-Timezone",
                                "X-Timezone-Offset"));

                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/SecurityUtils.java
package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.domain.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
            User u = (User) auth.getPrincipal();
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public static boolean isAdmin() {
        return getCurrentUser()
                .map(u -> u.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())))
                .orElse(false);
    }

    public static Optional<Long> currentCompanyId() {
        return getCurrentUser().map(u -> u.getCompany() != null ? u.getCompany().getId() : null);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/UserDetailsServiceImpl.java
package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
```

```java
// src/main/java/com/screenleads/backend/app/application/security/hibernate/CompanyFilterRequestEnabler.java
package com.screenleads.backend.app.application.security.hibernate;

import com.screenleads.backend.app.domain.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(20) // ejecuta después del filtro JWT
public class CompanyFilterRequestEnabler extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CompanyFilterRequestEnabler.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    public CompanyFilterRequestEnabler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                log.debug("Auth class={}, principal={}, authorities={}",
                        auth.getClass().getName(),
                        auth.getPrincipal(),
                        auth.getAuthorities());

                boolean isAdmin = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));

                Long companyId = resolveCompanyId(auth);
                log.debug("Resolved companyId={} (isAdmin={})", companyId, isAdmin);

                if (!isAdmin && companyId != null) {
                    Session session = entityManager.unwrap(Session.class);
                    var filter = session.enableFilter("companyFilter");
                    filter.setParameter("companyId", companyId);
                    log.info("Enabled companyFilter with companyId={}", companyId);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            try {
                Session s = entityManager.unwrap(Session.class);
                if (s.getEnabledFilter("companyFilter") != null) {
                    s.disableFilter("companyFilter");
                    log.debug("Disabled companyFilter at end of request");
                }
            } catch (Exception ignored) {
            }
        }
    }

    private Long resolveCompanyId(Authentication auth) {
        Object principal = auth.getPrincipal();

        // 1) Tu entidad de dominio como principal
        if (principal instanceof com.screenleads.backend.app.domain.model.User) {
            com.screenleads.backend.app.domain.model.User u = (com.screenleads.backend.app.domain.model.User) principal;
            return u.getCompany() != null ? u.getCompany().getId() : null;
        }

        // 2) UserDetails estándar
        if (principal instanceof UserDetails) {
            UserDetails ud = (UserDetails) principal;
            return userRepository.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        // 3) Principal como String (username), típico en JWT con claim "sub"
        if (principal instanceof String) {
            String username = (String) principal;
            return userRepository.findByUsername(username)
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        return null;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/jwt/AuthSecurityChecker.java
package com.screenleads.backend.app.application.security.jwt;

import com.screenleads.backend.app.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authSecurityChecker")
@RequiredArgsConstructor
public class AuthSecurityChecker {

    private final UserRepository userRepository;

    public boolean allowRegister() {
        // Si no hay usuarios, permitir el registro libre
        if (userRepository.count() == 0) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Si no hay autenticación válida o no tiene roles, denegar
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities() == null) {
            return false;
        }

        // Permitir solo si tiene ROLE_ADMIN
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isAuthenticated() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Si no hay autenticación válida o no tiene roles, denegar
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities() == null) {
            return false;
        } else {
            return true;
        }

    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/jwt/JwtAuthenticationFilter.java
package com.screenleads.backend.app.application.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT para autenticar peticiones entrantes.
 *
 * - Ignora rutas públicas (login/refresh, swagger, health, WS, OPTIONS).
 * - Extrae el token "Bearer ..." del header Authorization.
 * - Valida el token con JwtService y carga el usuario con UserDetailsService.
 * - Si es válido, fija la autenticación en el SecurityContext.
 * - Si es inválido/ausente, NO lanza excepción: deja que la cadena continúe y
 * que
 * el Security layer responda 401 en rutas que lo requieran.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // Rutas públicas (no requieren autenticación)
    private static final String[] WHITELIST = new String[] {
            // Auth: SOLO login y refresh son públicos (NO /auth/me)
            "/auth/login",
            "/auth/refresh",

            // Swagger / OpenAPI
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",

            // Health
            "/actuator/health",

            // WebSocket público (handshake/info)
            "/chat-socket/**",
            "/ws/status"
    };

    private final JwtService jwtService; // extractUsername(), isTokenValid(), ...
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String uri = request.getRequestURI();
        final String method = request.getMethod();

        try {
            // 0) Preflight siempre pasa
            if (HttpMethod.OPTIONS.matches(method)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 1) Whitelist: deja pasar sin tocar el SecurityContext
            if (isWhitelisted(uri)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2) Header Authorization: Bearer <token>
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                // Sin token -> continúa; si la ruta requiere auth, Security devolverá 401
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(BEARER_PREFIX.length());

            // 3) Extrae username del token
            final String username = jwtService.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 4) Valida token y construye autenticación
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    // Token inválido -> limpia contexto y sigue; endpoint autenticado devolverá 401
                    SecurityContextHolder.clearContext();
                }
            }

            // 5) Continua la cadena
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            // Limpia por seguridad y deja que el chain gestione la respuesta
            SecurityContextHolder.clearContext();
            // No relanzamos RuntimeException para evitar 500: el EntryPoint/AccessDenied se
            // encargará
            filterChain.doFilter(request, response);
        }
    }

    private boolean isWhitelisted(String uri) {
        for (String pattern : WHITELIST) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/jwt/JwtService.java
package com.screenleads.backend.app.application.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.User;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    public SecretKey getSigningKey() {
        return signingKey;
    }

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(User user) {
        return Jwts
                .builder()
                .subject(user.getUsername())
                .claim("roles", user.getAuthorities().stream()
                        .map(Object::toString)
                        .toList())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24h
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username)) && !isTokenExpired(token);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails.getUsername());
    }

    public String resolveToken(HttpServletRequest request) {
        final String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/application/security/websocket/AuthChannelInterceptor.java
package com.screenleads.backend.app.application.security.websocket;

import com.screenleads.backend.app.application.security.jwt.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthChannelInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                String username = jwtService.extractUsername(jwt);
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(authentication);
                    }
                }
            }
        }

        return message;
    }
}

```

