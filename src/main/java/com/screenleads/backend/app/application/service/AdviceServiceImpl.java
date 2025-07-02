package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;

@Service
public class AdviceServiceImpl implements AdviceService {

    private final AdviceRepository adviceRepository;
    private final MediaRepository mediaRepository;

    public AdviceServiceImpl(AdviceRepository adviceRepository, MediaRepository mediaRepository) {
        this.adviceRepository = adviceRepository;
        this.mediaRepository = mediaRepository;
    }

    @Override
    public List<AdviceDTO> getAllAdvices() {
        return adviceRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AdviceDTO> getAdviceById(Long id) {
        return adviceRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public AdviceDTO saveAdvice(AdviceDTO adviceDTO) {
        Advice advice = convertToEntity(adviceDTO);
        // Optional<Advice> exist = adviceRepository.findById(advice.getId());
        // if (exist.isPresent())
        // return convertToDTO(exist.get());
        Advice savedAdvice = adviceRepository.save(advice);
        return convertToDTO(savedAdvice);
    }

    @Override
    public AdviceDTO updateAdvice(Long id, AdviceDTO adviceDTO) {
        Advice advice = adviceRepository.findById(id).orElseThrow();
        advice.setCustomInterval(adviceDTO.customInterval());
        advice.setDescription(adviceDTO.description());
        advice.setInterval(adviceDTO.interval());
        advice.setMedia(mediaRepository.findById(adviceDTO.media().getId()).get());
        advice.setPromotion(adviceDTO.promotion());

        Advice updatedAdvice = adviceRepository.save(advice);
        return convertToDTO(updatedAdvice);
    }

    @Override
    public void deleteAdvice(Long id) {
        adviceRepository.deleteById(id);
    }

    // Convert Advice Entity to AdviceDTO
    private AdviceDTO convertToDTO(Advice advice) {
        Media media = null;
        Optional<Media> aux = mediaRepository.findById(advice.getMedia().getId());
        if (aux.isPresent()) {
            media = aux.get();
        }
        return new AdviceDTO(advice.getId(), advice.getDescription(), advice.getCustomInterval(), advice.getInterval(),
                media, advice.getPromotion());
    }

    // Convert AdviceDTO to Advice Entity
    private Advice convertToEntity(AdviceDTO adviceDTO) {
        Advice advice = new Advice();
        advice.setId(adviceDTO.id());
        advice.setDescription(adviceDTO.description());
        advice.setCustomInterval(adviceDTO.customInterval());
        advice.setInterval(adviceDTO.interval());
        advice.setMedia(mediaRepository.findById(adviceDTO.media().getId()).get());
        advice.setPromotion(adviceDTO.promotion());
        return advice;
    }
}
