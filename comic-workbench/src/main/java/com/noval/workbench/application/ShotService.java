package com.noval.workbench.application;

import com.noval.workbench.domain.model.Shot;
import com.noval.workbench.domain.model.ShotState;
import com.noval.workbench.domain.repository.ShotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 分镜应用服务
 */
@Service
public class ShotService {

    private final ShotRepository shotRepository;

    public ShotService(ShotRepository shotRepository) {
        this.shotRepository = shotRepository;
    }

    public List<Shot> listByNovelId(Long novelId) {
        return shotRepository.findByNovelId(novelId);
    }

    public Optional<Shot> getById(Long id) {
        return shotRepository.findById(id);
    }

    public Shot create(Shot shot) {
        return shotRepository.save(shot);
    }

    public Shot update(Long id, Shot shot) {
        return shotRepository.save(Shot.builder()
                .id(id)
                .novelId(shot.getNovelId())
                .sequence(shot.getSequence())
                .originalText(shot.getOriginalText())
                .sceneDescription(shot.getSceneDescription())
                .promptText(shot.getPromptText())
                .imageUrls(shot.getImageUrls())
                .tags(shot.getTags())
                .state(shot.getState())
                .build());
    }

    public void delete(Long id) {
        shotRepository.deleteById(id);
    }

    public void batchInfer(Long novelId) {
        List<Shot> shots = shotRepository.findByNovelId(novelId);
        for (Shot shot : shots) {
            if (shot.getState() == ShotState.PENDING) {
                Shot updated = Shot.builder()
                        .id(shot.getId())
                        .novelId(shot.getNovelId())
                        .sequence(shot.getSequence())
                        .originalText(shot.getOriginalText())
                        .sceneDescription(shot.getSceneDescription())
                        .promptText(shot.getPromptText())
                        .imageUrls(shot.getImageUrls())
                        .tags(shot.getTags())
                        .state(ShotState.INFERRING)
                        .build();
                shotRepository.save(updated);
            }
        }
    }

    public void batchGenerate(Long novelId) {
        List<Shot> shots = shotRepository.findByNovelId(novelId);
        for (Shot shot : shots) {
            if (shot.getState() == ShotState.INFERRED) {
                Shot updated = Shot.builder()
                        .id(shot.getId())
                        .novelId(shot.getNovelId())
                        .sequence(shot.getSequence())
                        .originalText(shot.getOriginalText())
                        .sceneDescription(shot.getSceneDescription())
                        .promptText(shot.getPromptText())
                        .imageUrls(shot.getImageUrls())
                        .tags(shot.getTags())
                        .state(ShotState.GENERATING)
                        .build();
                shotRepository.save(updated);
            }
        }
    }
}
