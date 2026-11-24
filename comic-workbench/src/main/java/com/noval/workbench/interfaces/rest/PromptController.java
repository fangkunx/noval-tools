package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.PromptService;
import com.noval.workbench.domain.model.Prompt;
import com.noval.workbench.interfaces.rest.dto.PromptRequest;
import com.noval.workbench.interfaces.rest.dto.PromptResponse;
import com.noval.workbench.interfaces.rest.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    public List<PromptResponse> list(@RequestParam(required = false) String category) {
        return promptService.listByCategory(category).stream()
                .map(PromptResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return promptService.getById(id)
                .map(p -> ResponseEntity.ok(PromptResponse.from(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public PromptResponse create(@RequestBody PromptRequest request) {
        Prompt prompt = promptService.create(request.toDomain());
        return PromptResponse.from(prompt);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PromptRequest request) {
        if (!promptService.getById(id).isPresent()) {
            return ResponseEntity.status(404).body(new ErrorResponse("Not Found", "提示词不存在"));
        }
        Prompt updated = promptService.update(id, request.toDomain());
        return ResponseEntity.ok(PromptResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promptService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
