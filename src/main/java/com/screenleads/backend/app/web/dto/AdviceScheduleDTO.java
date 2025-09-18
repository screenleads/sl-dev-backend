package com.screenleads.backend.app.web.dto;

import java.util.List;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdviceScheduleDTO {
    private Long id;
    private String startDate; // "YYYY-MM-DD" o null
    private String endDate;   // "YYYY-MM-DD" o null
    private List<AdviceTimeWindowDTO> windows;
}
