package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.screenleads.backend.app.application.service.AdviceService;
import com.screenleads.backend.app.application.service.DeviceService;
import com.screenleads.backend.app.web.dto.AdviceDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AdvicesController {
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    @Autowired
    private AdviceService adviceService;

    public AdvicesController(AdviceService adviceService) {
        this.adviceService = adviceService;
    }

    @CrossOrigin
    @GetMapping("/advices")
    public ResponseEntity<List<AdviceDTO>> getAllAdvices() {
        return ResponseEntity.ok(adviceService.getAllAdvices());
    }

    @CrossOrigin
    @GetMapping("/advices/visibles")
    public ResponseEntity<List<AdviceDTO>> getVisibleAdvicesNow() {
        return ResponseEntity.ok(adviceService.getVisibleAdvicesNow());
    }

    @CrossOrigin
    @GetMapping("/advices/{id}")
    public ResponseEntity<AdviceDTO> getAdviceById(@PathVariable Long id) {
        Optional<AdviceDTO> advice = adviceService.getAdviceById(id);
        return advice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping(value = "/advices")
    public ResponseEntity<AdviceDTO> createAdvice(@RequestBody AdviceDTO adviceDTO) {
        return ResponseEntity.ok(adviceService.saveAdvice(adviceDTO));
    }

    @CrossOrigin
    @PutMapping("/advices/{id}")
    public ResponseEntity<AdviceDTO> updateAdvice(@PathVariable Long id, @RequestBody AdviceDTO adviceDTO) {

        logger.info("adviceDTO object: {}", adviceDTO);
        AdviceDTO updatedAdvice = adviceService.updateAdvice(id, adviceDTO);
        return ResponseEntity.ok(updatedAdvice);

    }

    @CrossOrigin
    @DeleteMapping("/advices/{id}")
    public ResponseEntity<Void> deleteAdvice(@PathVariable Long id) {
        adviceService.deleteAdvice(id);
        return ResponseEntity.noContent().build();
    }
}