package com.noval.workbench.application;

import com.noval.workbench.domain.model.Novel;
import com.noval.workbench.domain.model.Shot;
import com.noval.workbench.domain.model.ShotState;
import com.noval.workbench.domain.repository.NovelRepository;
import com.noval.workbench.domain.repository.ShotRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 分镜应用服务
 */
@Service
public class ShotService {

    private final ShotRepository shotRepository;
    private final NovelRepository novelRepository;

    /** 匹配行首的序号，如 "1."、"1)"、"【1】"、"第1章"、"001 " 等 */
    private static final Pattern LEADING_NUMBER = Pattern.compile(
            "^\\s*(?:第[\\d一二三四五六七八九十百千]+[章节回幕篇话部]|" +
            "\\d{1,4}\\s*[.)、：:）\\]]\\s*|" +
            "【\\d+】\\s*|" +
            "\\[\\d+\\]\\s*|" +
            "\\d{1,4}\\s+)"
    );

    public ShotService(ShotRepository shotRepository, NovelRepository novelRepository) {
        this.shotRepository = shotRepository;
        this.novelRepository = novelRepository;
    }

    public List<Shot> listByNovelId(Long novelId) {
        return shotRepository.findByNovelId(novelId);
    }

    public List<Shot> listByNovelIdPaged(Long novelId, int page, int size) {
        if (size <= 0) {
            // size <= 0 表示查询全部
            return shotRepository.findByNovelId(novelId);
        }
        int offset = (page - 1) * size;
        return shotRepository.findByNovelIdPaged(novelId, offset, size);
    }

    public int countByNovelId(Long novelId) {
        return shotRepository.countByNovelId(novelId);
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
                .mainImageUrl(shot.getMainImageUrl())
                .altImageUrls(shot.getAltImageUrls())
                .tags(shot.getTags())
                .state(shot.getState())
                .build());
    }

    public void delete(Long id) {
        shotRepository.deleteById(id);
    }

    /**
     * 选择主图（从备选图中选择）
     */
    public Shot selectMainImage(Long shotId, String imageUrl) {
        Optional<Shot> opt = shotRepository.findById(shotId);
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("分镜不存在: " + shotId);
        }
        Shot shot = opt.get();
        // 将选中的图片设为主图
        return shotRepository.save(Shot.builder()
                .id(shot.getId())
                .novelId(shot.getNovelId())
                .sequence(shot.getSequence())
                .originalText(shot.getOriginalText())
                .sceneDescription(shot.getSceneDescription())
                .promptText(shot.getPromptText())
                .mainImageUrl(imageUrl)
                .altImageUrls(shot.getAltImageUrls())
                .tags(shot.getTags())
                .state(shot.getState())
                .build());
    }

    public void batchInfer(Long novelId, String scope, int page) {
        List<Shot> shots;
        if ("page".equals(scope)) {
            int pageSize = 10; // 默认页大小
            int offset = (page - 1) * pageSize;
            shots = shotRepository.findByNovelIdPaged(novelId, offset, pageSize);
        } else {
            shots = shotRepository.findByNovelId(novelId);
        }
        for (Shot shot : shots) {
            if (shot.getState() == ShotState.PENDING) {
                Shot updated = Shot.builder()
                        .id(shot.getId())
                        .novelId(shot.getNovelId())
                        .sequence(shot.getSequence())
                        .originalText(shot.getOriginalText())
                        .sceneDescription(shot.getSceneDescription())
                        .promptText(shot.getPromptText())
                        .mainImageUrl(shot.getMainImageUrl())
                        .altImageUrls(shot.getAltImageUrls())
                        .tags(shot.getTags())
                        .state(ShotState.INFERRING)
                        .build();
                shotRepository.save(updated);
            }
        }
    }

    public void batchGenerate(Long novelId, String scope, int page) {
        List<Shot> shots;
        if ("page".equals(scope)) {
            int pageSize = 10;
            int offset = (page - 1) * pageSize;
            shots = shotRepository.findByNovelIdPaged(novelId, offset, pageSize);
        } else {
            shots = shotRepository.findByNovelId(novelId);
        }
        for (Shot shot : shots) {
            if (shot.getState() == ShotState.INFERRED) {
                Shot updated = Shot.builder()
                        .id(shot.getId())
                        .novelId(shot.getNovelId())
                        .sequence(shot.getSequence())
                        .originalText(shot.getOriginalText())
                        .sceneDescription(shot.getSceneDescription())
                        .promptText(shot.getPromptText())
                        .mainImageUrl(shot.getMainImageUrl())
                        .altImageUrls(shot.getAltImageUrls())
                        .tags(shot.getTags())
                        .state(ShotState.GENERATING)
                        .build();
                shotRepository.save(updated);
            }
        }
    }

    /**
     * 从小说内容按行导入分镜（一行一个分镜）
     * 自动清理行首序号，空行跳过
     */
    public List<Shot> importFromNovel(Long novelId) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (!novelOpt.isPresent()) {
            throw new IllegalArgumentException("小说不存在: " + novelId);
        }
        Novel novel = novelOpt.get();
        String content = novel.getContent();
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 先删除该小说已有的分镜，再重新导入
        List<Shot> existing = shotRepository.findByNovelId(novelId);
        for (Shot s : existing) {
            shotRepository.deleteById(s.getId());
        }

        String[] lines = content.split("\\r?\\n");
        List<Shot> created = new ArrayList<>();
        int seq = 0;
        for (String line : lines) {
            String text = cleanLeadingNumber(line);
            if (text.trim().isEmpty()) {
                continue;
            }
            seq++;
            Shot shot = Shot.builder()
                    .novelId(novelId)
                    .sequence(seq)
                    .originalText(text.trim())
                    .sceneDescription("")
                    .promptText("")
                    .mainImageUrl(null)
                    .altImageUrls(new ArrayList<>())
                    .tags(new ArrayList<>())
                    .state(ShotState.PENDING)
                    .build();
            created.add(shotRepository.save(shot));
        }
        return created;
    }

    /**
     * 清理行首序号（支持 "1." "1)" "第1章" "【1】" "[1]" "001 " 等格式）
     */
    public static String cleanLeadingNumber(String line) {
        if (line == null) return "";
        return LEADING_NUMBER.matcher(line).replaceFirst("").trim();
    }
}
