package com.noval.workbench.application;

import com.noval.workbench.domain.model.Preset;
import com.noval.workbench.domain.repository.PresetRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 预设应用服务
 */
@Service
public class PresetService {

    private final PresetRepository presetRepository;

    public PresetService(PresetRepository presetRepository) {
        this.presetRepository = presetRepository;
    }

    public Optional<Preset> getByNovelId(Long novelId) {
        return presetRepository.findByNovelId(novelId);
    }

    public Preset save(Preset preset) {
        return presetRepository.save(preset);
    }

    public void deleteByNovelId(Long novelId) {
        presetRepository.deleteByNovelId(novelId);
    }
}
