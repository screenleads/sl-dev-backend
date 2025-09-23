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

import com.screenleads.backend.app.domain.model.PromotionLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionLeadRepository extends JpaRepository<PromotionLead, Long> {

    Optional<PromotionLead> findTopByPromotion_IdAndIdentifierOrderByCreatedAtDesc(Long promotionId, String identifier);
    Optional<PromotionLead> findByCouponCode(String couponCode);

    long countByPromotion_IdAndIdentifierAndCreatedAtAfter(Long promotionId, String identifier, ZonedDateTime after);
    long countByPromotionAndCustomer(Long promotionId, Long customerId);
    long countByPromotionAndCustomerSince(Long promotionId, Long customerId, Instant since);

boolean existsByPromotionIdAndIdentifier(Long promotionId,String identifier);
    
    List<PromotionLead> findByPromotion_IdOrderByCreatedAtDesc(Long promotionId);

    List<PromotionLead> findByPromotion_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long promotionId, ZonedDateTime from, ZonedDateTime to);
            List<PromotionLead> findByPromotionId(Long promotionId);
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

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username); // 👈 añadir

    boolean existsByEmail(String email);
}
```

