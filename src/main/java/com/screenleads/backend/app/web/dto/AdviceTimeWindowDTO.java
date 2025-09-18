package com.screenleads.backend.app.web.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdviceTimeWindowDTO {
    private Long id;
    private String weekday;  // MONDAY..SUNDAY
    private String fromTime; // "HH:mm[:ss]"
    private String toTime;   // "HH:mm[:ss]"
}
