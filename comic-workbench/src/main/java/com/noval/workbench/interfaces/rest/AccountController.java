package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.AccountService;
import com.noval.workbench.domain.model.Account;
import com.noval.workbench.interfaces.rest.dto.AccountRequest;
import com.noval.workbench.interfaces.rest.dto.AccountResponse;
import com.noval.workbench.interfaces.rest.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.listAll().stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return accountService.getById(id)
                .map(a -> ResponseEntity.ok(AccountResponse.from(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public AccountResponse create(@RequestBody AccountRequest request) {
        Account account = accountService.create(request.toDomain());
        return AccountResponse.from(account);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AccountRequest request) {
        if (!accountService.getById(id).isPresent()) {
            return ResponseEntity.status(404).body(new ErrorResponse("Not Found", "账号不存在"));
        }
        Account updated = accountService.update(id, request.toDomain());
        return ResponseEntity.ok(AccountResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleEnabled(@PathVariable Long id) {
        accountService.toggleEnabled(id);
        return ResponseEntity.ok().build();
    }
}
