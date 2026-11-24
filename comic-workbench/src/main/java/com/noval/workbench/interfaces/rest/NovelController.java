package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.NovelService;
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

    public NovelController(NovelService novelService) {
        this.novelService = novelService;
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
            return ResponseEntity.status(404).body(new ErrorResponse("Not Found", "小说不存在"));
        }
        Novel updated = novelService.update(id, request.toDomain());
        return ResponseEntity.ok(NovelResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        novelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
