package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Shot;
import com.noval.workbench.domain.model.ShotState;

import java.util.List;

/**
 * 分镜响应 DTO
 */
public class ShotResponse {
    private Long id;
    private Long novelId;
    private int sequence;
    private String originalText;
    private String sceneDescription;
    private String promptText;
    private String mainImageUrl;
    private List<String> altImageUrls;
    private List<String> tags;
    private String state;

    public static ShotResponse from(Shot shot) {
        ShotResponse resp = new ShotResponse();
        resp.id = shot.getId();
        resp.novelId = shot.getNovelId();
        resp.sequence = shot.getSequence();
        resp.originalText = shot.getOriginalText();
        resp.sceneDescription = shot.getSceneDescription();
        resp.promptText = shot.getPromptText();
        resp.mainImageUrl = shot.getMainImageUrl();
        resp.altImageUrls = shot.getAltImageUrls();
        resp.tags = shot.getTags();
        resp.state = shot.getState().name();
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }
    public List<String> getAltImageUrls() { return altImageUrls; }
    public void setAltImageUrls(List<String> altImageUrls) { this.altImageUrls = altImageUrls; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
