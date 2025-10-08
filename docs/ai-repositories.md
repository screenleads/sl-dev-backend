# Repositorios — snapshot incrustado

> Interfaces de Spring Data JPA.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/AdviceRepository.java
package com.screenleads.backend.app.domain.repositories;


import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;


public interface AdviceRepository extends JpaRepository<Advice, Long> {


@EntityGraph(attributePaths = {"media", "promotion", "schedules", "schedules.windows"})
List<Advice> findByCompany(Company company);
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/AdviceTimeWindowRepository.java
package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.AdviceTimeWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public interface AdviceTimeWindowRepository extends JpaRepository<AdviceTimeWindow, Long> {

    /**
     * ¿Existe al menos una ventana activa para un anuncio dado, en una fecha/hora concreta?
     * Se comprueba:
     *  - que el día de la semana coincida
     *  - que la fecha esté dentro del rango del schedule (startDate/endDate pueden ser null)
     *  - que la hora esté dentro de una ventana [fromTime, toTime) (fin exclusivo)
     */
    @Query("""
        SELECT (COUNT(w.id) > 0)
          FROM Advice a
          JOIN a.schedules s
          JOIN s.windows w
         WHERE a.id = :adviceId
           AND w.weekday = :dow
           AND (:date >= s.startDate OR s.startDate IS NULL)
           AND (:date <= s.endDate   OR s.endDate   IS NULL)
           AND :time >= w.fromTime
           AND :time <  w.toTime
        """)
    boolean existsActive(@Param("adviceId") Long adviceId,
                         @Param("dow") DayOfWeek dow,
                         @Param("date") LocalDate date,
                         @Param("time") LocalTime time);
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/AppEntityAttributeRepository.java
// src/main/java/.../domain/repositories/AppEntityAttributeRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;

public interface AppEntityAttributeRepository extends JpaRepository<AppEntityAttribute, Long> {
    List<AppEntityAttribute> findByAppEntityIdOrderByListOrderAscIdAsc(Long appEntityId);

    Optional<AppEntityAttribute> findByAppEntityIdAndName(Long appEntityId, String name);
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/AppEntityRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.AppEntity;

public interface AppEntityRepository extends JpaRepository<AppEntity, Long> {

    Optional<AppEntity> findByResource(String resource);

    // Para reordenamiento de menú
    List<AppEntity> findByVisibleInMenuTrueOrderBySortOrderAsc();

    // Para cargar atributos al reordenar
    @EntityGraph(attributePaths = "attributes")
    Optional<AppEntity> findWithAttributesById(Long id);
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/AppVersionRepository.java
package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.AppVersion;

import java.util.Optional;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    Optional<AppVersion> findTopByPlatformOrderByIdDesc(String platform);
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/CompanyRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);

    boolean existsByName(String name);
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/CustomerRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCompanyIdAndIdentifierTypeAndIdentifier(
        Long companyId, LeadIdentifierType identifierType, String identifier
    );

    List<Customer> findByCompanyId(Long companyId);

    List<Customer> findByCompanyIdAndIdentifierContainingIgnoreCase(Long companyId, String identifierPart);
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/DeviceRepository.java
package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Device;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    boolean existsByUuid(String uuid);

    Device findByUuid(String uuid); // ya lo usabas

    Optional<Device> findOptionalByUuid(String uuid); // para 404 limpio
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/DeviceTypeRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.DeviceType;

public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {
    Optional<DeviceType> findByType(String type);
    boolean existsByType(String type);
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/MediaRepository.java
package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.screenleads.backend.app.domain.model.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {
    Optional<Media> findBySrc(String src); // útil si src es único
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/MediaTypeRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.MediaType;

public interface MediaTypeRepository extends JpaRepository<MediaType, Long> {
    Optional<MediaType> findByExtension(String extension);

    Optional<MediaType> findByType(String type);

    boolean existsByExtension(String extension);

    boolean existsByType(String type);

}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/PromotionLeadRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.CouponStatus;
import com.screenleads.backend.app.domain.model.PromotionLead;

public interface PromotionLeadRepository extends JpaRepository<PromotionLead, Long> {

    // ---- Búsquedas básicas ----
    List<PromotionLead> findByPromotionId(Long promotionId);

    Optional<PromotionLead> findByCouponCode(String couponCode);
    boolean existsByCouponCode(String couponCode);

    // Ambos órdenes por conveniencia
    Optional<PromotionLead> findByIdentifierAndPromotionId(String identifier, Long promotionId);
    Optional<PromotionLead> findByPromotionIdAndIdentifier(Long promotionId, String identifier);

    // *** Método que faltaba y causa el fallo de compilación ***
    boolean existsByPromotionIdAndIdentifier(Long promotionId, String identifier);

    Optional<PromotionLead> findTopByPromotionIdAndCustomerIdOrderByCreatedAtDesc(
            Long promotionId, Long customerId);

    // ---- Conteos / límites por cliente y promo ----
    long countByPromotionIdAndCustomerId(Long promotionId, Long customerId);

    long countByPromotionIdAndCustomerIdAndCouponStatus(
            Long promotionId, Long customerId, CouponStatus couponStatus);

    Optional<PromotionLead> findByPromotionIdAndCustomerIdAndCouponStatus(
            Long promotionId, Long customerId, CouponStatus couponStatus);

    // Conteo en rango temporal (útil para ventanas deslizantes)
    @Query("""
        select count(pl) from PromotionLead pl
        where pl.promotion.id = :promotionId
          and pl.customer.id  = :customerId
          and pl.createdAt between :from and :to
    """)
    long countByPromotionIdAndCustomerIdBetweenDates(@Param("promotionId") Long promotionId,
                                                     @Param("customerId") Long customerId,
                                                     @Param("from") Instant from,
                                                     @Param("to") Instant to);

    // Conteo desde un instante (Since = createdAt >= :since)
    @Query("""
        select count(pl) from PromotionLead pl
        where pl.promotion.id = :promotionId
          and pl.customer.id  = :customerId
          and pl.createdAt    >= :since
    """)
    long countByPromotionAndCustomerSince(@Param("promotionId") Long promotionId,
                                          @Param("customerId") Long customerId,
                                          @Param("since") Instant since);
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/PromotionRepository.java
package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/RoleRepository.java
package com.screenleads.backend.app.domain.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRole(String role);

    boolean existsByRole(String role);
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/repositories/UserRepository.java
package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"company", "profileImage"})
    Optional<User> findWithCompanyAndProfileImageByUsername(String username);

    boolean existsByUsername(String username); // 👈 añadir

    boolean existsByEmail(String email);
}
```

