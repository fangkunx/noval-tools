package com.noval.workbench.application;

import com.noval.workbench.domain.model.ComicSetting;
import com.noval.workbench.domain.repository.ComicSettingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 漫画设置应用服务
 */
@Service
public class ComicSettingService {

    private final ComicSettingRepository comicSettingRepository;

    public ComicSettingService(ComicSettingRepository comicSettingRepository) {
        this.comicSettingRepository = comicSettingRepository;
    }

    public Optional<ComicSetting> getByNovelId(Long novelId) {
        return comicSettingRepository.findByNovelId(novelId);
    }

    public ComicSetting save(ComicSetting setting) {
        return comicSettingRepository.save(setting);
    }
}
