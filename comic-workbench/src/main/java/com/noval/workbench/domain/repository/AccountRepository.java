package com.noval.workbench.domain.repository;

import com.noval.workbench.domain.model.Account;
import java.util.List;
import java.util.Optional;

/**
 * 账号仓储接口
 */
public interface AccountRepository {
    List<Account> findAll();
    Optional<Account> findById(Long id);
    Account save(Account account);
    void deleteById(Long id);
}
