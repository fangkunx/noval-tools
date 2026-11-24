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
    public List<ShotResponse> listByNovel(@RequestParam Long novelId) {
        return shotService.listByNovelId(novelId).stream()
                .map(ShotResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return shotService.getById(id)
                .map(s -> ResponseEntity.ok(ShotResponse.from(s)))
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
            return ResponseEntity.status(404).body(new ErrorResponse("Not Found", "分镜不存在"));
        }
        Shot updated = shotService.update(id, request.toDomain());
        return ResponseEntity.ok(ShotResponse.from(updated));
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
}
