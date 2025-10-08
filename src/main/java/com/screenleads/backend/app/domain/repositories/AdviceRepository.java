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