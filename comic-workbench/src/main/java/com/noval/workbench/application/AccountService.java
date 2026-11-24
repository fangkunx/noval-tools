package com.noval.workbench.application;

import com.noval.workbench.domain.model.Account;
import com.noval.workbench.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 账号应用服务
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> listAll() {
        return accountRepository.findAll();
    }

    public Optional<Account> getById(Long id) {
        return accountRepository.findById(id);
    }

    public Account create(Account account) {
        return accountRepository.save(account);
    }

    public Account update(Long id, Account account) {
        return accountRepository.save(Account.builder()
                .id(id)
                .name(account.getName())
                .protocol(account.getProtocol())
                .apiUrl(account.getApiUrl())
                .apiKey(account.getApiKey())
                .modelId(account.getModelId())
                .displayName(account.getDisplayName())
                .capabilities(account.getCapabilities())
                .enabled(account.isEnabled())
                .vip(account.isVip())
                .build());
    }

    public void delete(Long id) {
        accountRepository.deleteById(id);
    }

    public void toggleEnabled(Long id) {
        accountRepository.findById(id).ifPresent(account -> {
            Account updated = Account.builder()
                    .id(account.getId())
                    .name(account.getName())
                    .protocol(account.getProtocol())
                    .apiUrl(account.getApiUrl())
                    .apiKey(account.getApiKey())
                    .modelId(account.getModelId())
                    .displayName(account.getDisplayName())
                    .capabilities(account.getCapabilities())
                    .enabled(!account.isEnabled())
                    .vip(account.isVip())
                    .build();
            accountRepository.save(updated);
        });
    }
}
