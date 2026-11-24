package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.NovelService;
import com.noval.workbench.application.ShotService;
import com.noval.workbench.domain.model.Novel;
import com.noval.workbench.interfaces.rest.dto.NovelRequest;
import com.noval.workbench.interfaces.rest.dto.NovelResponse;
import com.noval.workbench.interfaces.rest.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/novels")
public class NovelController {

    private final NovelService novelService;
    private final ShotService shotService;

    public NovelController(NovelService novelService, ShotService shotService) {
        this.novelService = novelService;
        this.shotService = shotService;
    }

    @GetMapping
    public List<NovelResponse> list() {
        return novelService.listAll().stream()
                .map(NovelResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return novelService.getById(id)
                .map(n -> ResponseEntity.ok(NovelResponse.from(n)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public NovelResponse create(@RequestBody NovelRequest request) {
        Novel novel = novelService.create(request.toDomain());
        return NovelResponse.from(novel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody NovelRequest request) {
        if (!novelService.getById(id).isPresent()) {
            return ResponseEntity.status(404).body((Object) new ErrorResponse("Not Found", "小说不存在"));
        }
        Novel updated = novelService.update(id, request.toDomain());
        return ResponseEntity.ok((Object) NovelResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        novelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 清理小说内容每行开头的序号
     * POST /api/novels/{id}/clean-numbers
     * 返回清理后的小说响应（已持久化）
     */
    @PostMapping("/{id}/clean-numbers")
    public ResponseEntity<?> cleanLeadingNumbers(@PathVariable Long id) {
        java.util.Optional<Novel> novelOpt = novelService.getById(id);
        if (!novelOpt.isPresent()) {
            return ResponseEntity.status(404).body((Object) new ErrorResponse("Not Found", "小说不存在"));
        }
        Novel novel = novelOpt.get();
        String content = novel.getContent();
        if (content == null || content.isEmpty()) {
            return ResponseEntity.ok((Object) NovelResponse.from(novel));
        }
        String cleaned = java.util.Arrays.stream(content.split("\\r?\\n", -1))
                .map(ShotService::cleanLeadingNumber)
                .collect(Collectors.joining("\n"));
        Novel updated = novelService.update(id, Novel.builder()
                .title(novel.getTitle())
                .content(cleaned)
                .source(novel.getSource())
                .vip(novel.isVip())
                .status(novel.getStatus())
                .build());
        return ResponseEntity.ok((Object) NovelResponse.from(updated));
    }

    /**
     * 从小说内容按行导入分镜
     * POST /api/novels/{id}/import-shots
     */
    @PostMapping("/{id}/import-shots")
    public ResponseEntity<?> importShots(@PathVariable Long id) {
        if (!novelService.getById(id).isPresent()) {
            return ResponseEntity.status(404).body((Object) new ErrorResponse("Not Found", "小说不存在"));
        }
        try {
            int count = shotService.importFromNovel(id).size();
            return ResponseEntity.ok((Object) new ImportResult(count));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body((Object) new ErrorResponse("Bad Request", e.getMessage()));
        }
    }

    /** 导入分镜结果 */
    public static class ImportResult {
        private final int importedCount;
        public ImportResult(int importedCount) { this.importedCount = importedCount; }
        public int getImportedCount() { return importedCount; }
    }
}
