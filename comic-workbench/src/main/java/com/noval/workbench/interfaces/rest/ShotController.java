package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.ShotService;
import com.noval.workbench.domain.model.Shot;
import com.noval.workbench.interfaces.rest.dto.ShotRequest;
import com.noval.workbench.interfaces.rest.dto.ShotResponse;
import com.noval.workbench.interfaces.rest.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shots")
public class ShotController {

    private final ShotService shotService;

    public ShotController(ShotService shotService) {
        this.shotService = shotService;
    }

    @GetMapping
    public ResponseEntity<?> listByNovel(
            @RequestParam Long novelId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "0") int size) {
        if (size <= 0) {
            // size=0 表示查询全部（兼容旧逻辑）
            List<ShotResponse> all = shotService.listByNovelId(novelId).stream()
                    .map(ShotResponse::from)
                    .collect(Collectors.toList());
            PageResult result = new PageResult();
            result.data = all;
            result.total = all.size();
            result.page = 1;
            result.size = all.size();
            return ResponseEntity.ok((Object) result);
        }
        List<ShotResponse> paged = shotService.listByNovelIdPaged(novelId, page, size).stream()
                .map(ShotResponse::from)
                .collect(Collectors.toList());
        int total = shotService.countByNovelId(novelId);
        PageResult result = new PageResult();
        result.data = paged;
        result.total = total;
        result.page = page;
        result.size = size;
        return ResponseEntity.ok((Object) result);
    }

    /** 分页结果 */
    public static class PageResult {
        public List<ShotResponse> data;
        public int total;
        public int page;
        public int size;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return shotService.getById(id)
                .map(s -> ResponseEntity.ok((Object) ShotResponse.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ShotResponse create(@RequestBody ShotRequest request) {
        Shot shot = shotService.create(request.toDomain());
        return ShotResponse.from(shot);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ShotRequest request) {
        if (!shotService.getById(id).isPresent()) {
            return ResponseEntity.status(404).body((Object) new ErrorResponse("Not Found", "分镜不存在"));
        }
        Shot updated = shotService.update(id, request.toDomain());
        return ResponseEntity.ok((Object) ShotResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shotService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch-infer")
    public ResponseEntity<Void> batchInfer(@RequestParam Long novelId) {
        shotService.batchInfer(novelId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-generate")
    public ResponseEntity<Void> batchGenerate(@RequestParam Long novelId) {
        shotService.batchGenerate(novelId);
        return ResponseEntity.ok().build();
    }

    /**
     * 选择主图（从备选图中选择）
     * POST /api/shots/{id}/select-main-image?imageUrl=xxx
     */
    @PostMapping("/{id}/select-main-image")
    public ResponseEntity<?> selectMainImage(@PathVariable Long id, @RequestParam String imageUrl) {
        try {
            Shot updated = shotService.selectMainImage(id, imageUrl);
            return ResponseEntity.ok((Object) ShotResponse.from(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body((Object) new ErrorResponse("Bad Request", e.getMessage()));
        }
    }
}
