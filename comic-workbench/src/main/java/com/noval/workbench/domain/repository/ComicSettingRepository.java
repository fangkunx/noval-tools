package com.noval.workbench.domain.repository;

import com.noval.workbench.domain.model.ComicSetting;
import java.util.Optional;

/**
 * 漫画设置仓储接口
 */
public interface ComicSettingRepository {
    Optional<ComicSetting> findByNovelId(Long novelId);
    ComicSetting save(ComicSetting setting);
}
