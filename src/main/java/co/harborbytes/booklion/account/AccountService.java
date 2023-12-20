package co.harborbytes.booklion.account;


import co.harborbytes.booklion.exception.DomainEntityNotFoundException;
import co.harborbytes.booklion.exception.DomainEntityValidationException;
import jakarta.persistence.Column;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.validation.ConstraintViolation;
import org.springframework.validation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Service
public class AccountService {
    private final AccountRepository repo;
    private final AccountMapper mapper;
    private final Validator validator;

    @Autowired
    public AccountService(AccountRepository repo, AccountMapper mapper, Validator validator) {
        this.repo = repo;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional
    public AccountDTO createAccount(AccountDTO account) {
        Account mappedAccount = mapper.dtoToAccount(account);
        mappedAccount.setId(null);
        return mapper.accountToDto(repo.save(mappedAccount));
    }

    public AccountDTO findAccountById(Long id) {
        Account accountToFind = repo.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException(Account.class.getSimpleName(), "id", id.toString()));

        return mapper.accountToDto(accountToFind);
    }

    public Account findActualAccountById(Long id) {
        Account accountToFind = repo.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException(Account.class.getSimpleName(), "id", id.toString()));

        return accountToFind;
    }

    public AccountDTO findAccountByNumber(String number) {
        Account accountToFind = repo.findByNumber(number)
                .orElseThrow(() -> new DomainEntityNotFoundException(Account.class.getSimpleName(), "number", number));

        return mapper.accountToDto(accountToFind);
    }

    public Page<AccountDTO> findAllAccounts(Pageable pageable) {
        return repo.findAll(pageable)
                .map(mapper::accountToDto);
    }

    @Transactional
    public AccountDTO updateAccount(AccountDTO accountDto, AtomicBoolean didCreate) {

        Optional<Account> accountToUpdate = repo.findById(accountDto.getId());
        didCreate.set(false);

        if (accountToUpdate.isEmpty()) {
            Account mappedAccount = mapper.dtoToAccount(accountDto);
            didCreate.set(true);
            return mapper.accountToDto(repo.save(mappedAccount));
        }

        Account account = accountToUpdate.get();
        mapper.updateAccountFromDto(accountDto, account);
        return mapper.accountToDto(account);
    }

    @Transactional
    public AccountDTO partiallyUpdateAccount(Map<String, Object> incompleteAccount, Long id) {

        Account accountToUpdate = repo.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException(Account.class.getSimpleName(), "id", id.toString()));


        Field[] accountFields = Account.class.getDeclaredFields();
        BindingResult result = new BeanPropertyBindingResult(accountToUpdate, "accountToUpdate");

        for (Field accountField : accountFields) {

            if (!incompleteAccount.keySet().contains(accountField.getName()))
                continue;

            if (Modifier.isStatic(accountField.getModifiers()))
                continue;

            if (accountField.getName().equals("id"))
                continue;

            Column column = accountField.getAnnotation(Column.class);

            if (column == null)
                continue;

            if (!column.updatable())
                continue;

            Object jsonValue = incompleteAccount.get(accountField.getName());
            accountField.setAccessible(true);

            try {
                accountField.set(accountToUpdate, jsonValue);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                result.addError(
                        new FieldError(
                                "accountToUpdate",
                                accountField.getName(),
                                String.format("field cannot be set with  \"%s\"", jsonValue)));
            }
        }

        validator.validate(accountToUpdate, result);
        if (result.hasErrors())
            throw new DomainEntityValidationException(result);

        return mapper.accountToDto(accountToUpdate);
    }


}
