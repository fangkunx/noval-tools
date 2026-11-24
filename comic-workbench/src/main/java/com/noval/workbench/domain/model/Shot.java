package com.noval.workbench.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * 分镜实体
 */
public class Shot {
    private final Long id;
    private final Long novelId;
    private final int sequence;
    private final String originalText;
    private final String sceneDescription;
    private final String promptText;
    private final String mainImageUrl;
    private final List<String> altImageUrls;
    private final List<String> tags;
    private final ShotState state;

    private Shot(Builder builder) {
        this.id = builder.id;
        this.novelId = builder.novelId;
        this.sequence = builder.sequence;
        this.originalText = builder.originalText;
        this.sceneDescription = builder.sceneDescription;
        this.promptText = builder.promptText;
        this.mainImageUrl = builder.mainImageUrl;
        this.altImageUrls = builder.altImageUrls == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(builder.altImageUrls);
        this.tags = builder.tags == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(builder.tags);
        this.state = builder.state;
    }

    public Long getId() { return id; }
    public Long getNovelId() { return novelId; }
    public int getSequence() { return sequence; }
    public String getOriginalText() { return originalText; }
    public String getSceneDescription() { return sceneDescription; }
    public String getPromptText() { return promptText; }
    public String getMainImageUrl() { return mainImageUrl; }
    public List<String> getAltImageUrls() { return altImageUrls; }
    public List<String> getTags() { return tags; }
    public ShotState getState() { return state; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long novelId;
        private int sequence;
        private String originalText;
        private String sceneDescription;
        private String promptText;
        private String mainImageUrl;
        private List<String> altImageUrls;
        private List<String> tags;
        private ShotState state = ShotState.PENDING;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder novelId(Long novelId) { this.novelId = novelId; return this; }
        public Builder sequence(int sequence) { this.sequence = sequence; return this; }
        public Builder originalText(String originalText) { this.originalText = originalText; return this; }
        public Builder sceneDescription(String sceneDescription) { this.sceneDescription = sceneDescription; return this; }
        public Builder promptText(String promptText) { this.promptText = promptText; return this; }
        public Builder mainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; return this; }
        public Builder altImageUrls(List<String> altImageUrls) { this.altImageUrls = altImageUrls; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder state(ShotState state) { this.state = state; return this; }
        public Shot build() { return new Shot(this); }
    }
}
