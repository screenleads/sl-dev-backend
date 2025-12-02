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
    @Query("SELECT (COUNT(w.id) > 0) " +
           "FROM Advice a " +
           "JOIN a.schedules s " +
           "JOIN s.windows w " +
           "WHERE a.id = :adviceId " +
           "AND w.weekday = :dow " +
           "AND (:date >= s.startDate OR s.startDate IS NULL) " +
           "AND (:date <= s.endDate OR s.endDate IS NULL) " +
           "AND :time >= w.fromTime " +
           "AND :time < w.toTime")
    boolean existsActive(@Param("adviceId") Long adviceId,
                         @Param("dow") DayOfWeek dow,
                         @Param("date") LocalDate date,
                         @Param("time") LocalTime time);
}
