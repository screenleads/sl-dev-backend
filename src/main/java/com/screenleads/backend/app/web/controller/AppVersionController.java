package com.screenleads.backend.app.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.screenleads.backend.app.application.service.AppVersionService;
import com.screenleads.backend.app.web.dto.AppVersionDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/app-versions")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService service;

    @PostMapping
    public AppVersionDTO save(@RequestBody AppVersionDTO dto) {
        return service.save(dto);
    }

    @GetMapping
    public List<AppVersionDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AppVersionDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        service.deleteById(id);
    }

    @GetMapping("/latest/{platform}")
    public AppVersionDTO getLatestVersion(@PathVariable String platform) {
        return service.getLatestVersion(platform);
    }
}