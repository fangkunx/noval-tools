package com.noval.workbench.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * 预设实体：剧情梗概 + 角色/场景/道具预设
 */
public class Preset {
    private final Long id;
    private final Long novelId;
    private final String plotSummary;
    private final List<PresetItem> characters;
    private final List<PresetItem> scenes;
    private final List<PresetItem> props;

    private Preset(Builder builder) {
        this.id = builder.id;
        this.novelId = builder.novelId;
        this.plotSummary = builder.plotSummary;
        this.characters = builder.characters == null
                ? Collections.emptyList() : Collections.unmodifiableList(builder.characters);
        this.scenes = builder.scenes == null
                ? Collections.emptyList() : Collections.unmodifiableList(builder.scenes);
        this.props = builder.props == null
                ? Collections.emptyList() : Collections.unmodifiableList(builder.props);
    }

    public Long getId() { return id; }
    public Long getNovelId() { return novelId; }
    public String getPlotSummary() { return plotSummary; }
    public List<PresetItem> getCharacters() { return characters; }
    public List<PresetItem> getScenes() { return scenes; }
    public List<PresetItem> getProps() { return props; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long novelId;
        private String plotSummary;
        private List<PresetItem> characters;
        private List<PresetItem> scenes;
        private List<PresetItem> props;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder novelId(Long novelId) { this.novelId = novelId; return this; }
        public Builder plotSummary(String plotSummary) { this.plotSummary = plotSummary; return this; }
        public Builder characters(List<PresetItem> characters) { this.characters = characters; return this; }
        public Builder scenes(List<PresetItem> scenes) { this.scenes = scenes; return this; }
        public Builder props(List<PresetItem> props) { this.props = props; return this; }
        public Preset build() { return new Preset(this); }
    }
}
