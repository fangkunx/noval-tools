package com.noval.workbench.application;

import com.noval.workbench.domain.model.ArtStyle;
import com.noval.workbench.domain.repository.ArtStyleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 画风应用服务
 */
@Service
public class ArtStyleService {

    private final ArtStyleRepository artStyleRepository;

    public ArtStyleService(ArtStyleRepository artStyleRepository) {
        this.artStyleRepository = artStyleRepository;
    }

    public List<ArtStyle> listAll() {
        return artStyleRepository.findAll();
    }

    public Optional<ArtStyle> getById(Long id) {
        return artStyleRepository.findById(id);
    }

    public ArtStyle create(ArtStyle artStyle) {
        return artStyleRepository.save(artStyle);
    }

    public ArtStyle update(Long id, ArtStyle artStyle) {
        return artStyleRepository.save(ArtStyle.builder()
                .id(id)
                .name(artStyle.getName())
                .description(artStyle.getDescription())
                .imageUrl(artStyle.getImageUrl())
                .usageCount(artStyle.getUsageCount())
                .build());
    }

    public void delete(Long id) {
        artStyleRepository.deleteById(id);
    }
}
