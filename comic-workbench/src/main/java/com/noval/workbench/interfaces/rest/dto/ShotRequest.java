package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Shot;
import com.noval.workbench.domain.model.ShotState;

import java.util.List;

/**
 * 分镜请求 DTO
 */
public class ShotRequest {
    private Long novelId;
    private int sequence;
    private String originalText;
    private String sceneDescription;
    private String promptText;
    private List<String> imageUrls;
    private List<String> tags;
    private String state;

    public Shot toDomain() {
        return Shot.builder()
                .novelId(novelId)
                .sequence(sequence)
                .originalText(originalText)
                .sceneDescription(sceneDescription)
                .promptText(promptText)
                .imageUrls(imageUrls)
                .tags(tags)
                .state(state != null ? ShotState.valueOf(state) : ShotState.PENDING)
                .build();
    }

    public Long getNovelId() { return novelId; }
    public void setNovelId(Long novelId) { this.novelId = novelId; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    public String getSceneDescription() { return sceneDescription; }
    public void setSceneDescription(String sceneDescription) { this.sceneDescription = sceneDescription; }
    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
