package com.screenleads.backend.app.web.dto;

import com.screenleads.backend.app.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private User user;
}