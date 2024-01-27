package co.harborbytes.booklion.account;


import co.harborbytes.booklion.exception.DomainEntityNotFoundException;
import co.harborbytes.booklion.exception.DomainEntityValidationException;
import co.harborbytes.booklion.transaction.Transaction;
import co.harborbytes.booklion.transaction.TransactionLine;
import co.harborbytes.booklion.transaction.TransactionRepository;
import jakarta.persistence.Column;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.validation.*;

@Service
public class AccountService {
    private final AccountRepository repo;
    private final AccountMapper mapper;
    private final TransactionRepository transactionRepository;
    private final Validator validator;

    @Autowired
    public AccountService(AccountRepository repo, AccountMapper mapper, Validator validator, TransactionRepository transactionRepository) {
        this.repo = repo;
        this.mapper = mapper;
        this.validator = validator;
        this.transactionRepository = transactionRepository;
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


    public List<AccountOverviewByType> getAccountOverviewGroupedByAccountType(Long userId, Instant from) {
        List<Transaction> transactionList =  transactionRepository.findAllTransactionsByUserIdAndCreatedAtAfter(userId, from);

        Map<AccountType, AccountOverviewByType> overviewMap = new HashMap<>();

        for(Transaction transaction : transactionList){
            for(TransactionLine transactionLine: transaction.getLines()){


                AccountType accountType = transactionLine.getAccount().getAccountType();
                AccountOverviewByType overview = overviewMap.get(accountType);

                if(overview == null){
                    overview = new AccountOverviewByType();
                    overviewMap.put(accountType, overview);
                }

                if(overview.getDateLastTransaction() == null){
                    overview.setDateLastTransaction(transaction.getCreatedAt());
                }

                if(transaction.getCreatedAt().isAfter(overview.getDateLastTransaction())){
                    overview.setDateLastTransaction(transaction.getCreatedAt());
                }

                overview.setTransactionCount(overview.getTransactionCount() + 1);

                overview.setType(accountType.toString());

                if(accountType == AccountType.ASSETS || accountType == AccountType.EXPENSES){
                    overview.setBalance(overview.getBalance().add(transactionLine.getDebitAmount()).subtract(transactionLine.getCreditAmount()));
                }

                if(accountType == AccountType.LIABILITIES || accountType == AccountType.EQUITY || accountType == AccountType.REVENUE ){
                    overview.setBalance(overview.getBalance().add(transactionLine.getCreditAmount()).subtract(transactionLine.getDebitAmount()));
                }
            }
        }



        return new ArrayList<>(overviewMap.values());
    }

    public List<IndividualAccountOverview> getAccountOverviewPerAccount(Long userId, Instant from) {
        List<Transaction> transactionList =  transactionRepository.findAllTransactionsByUserIdAndCreatedAtAfter(userId, from);

        Map<String, IndividualAccountOverview> overviewMap = new HashMap<>();

        for(Transaction transaction : transactionList){
            for(TransactionLine transactionLine: transaction.getLines()) {

                String accountNumber = transactionLine.getAccount().getNumber();
                IndividualAccountOverview overview = overviewMap.get(accountNumber);

                if(overview == null){
                    overview = new IndividualAccountOverview();
                    overviewMap.put(accountNumber, overview);
                }

                if(overview.getDateLastTransaction() == null){
                    overview.setDateLastTransaction(transaction.getCreatedAt());
                }

                if(transaction.getCreatedAt().isAfter(overview.getDateLastTransaction())){
                    overview.setDateLastTransaction(transaction.getCreatedAt());
                }

                overview.setTransactionCount(overview.getTransactionCount() + 1);

                AccountType accountType = transactionLine.getAccount().getAccountType();
                overview.setType(accountType.toString());

                if(accountType == AccountType.ASSETS || accountType == AccountType.EXPENSES){
                    overview.setBalance(overview.getBalance().add(transactionLine.getDebitAmount()).subtract(transactionLine.getCreditAmount()));
                }

                if(accountType == AccountType.LIABILITIES || accountType == AccountType.EQUITY || accountType == AccountType.REVENUE ){
                    overview.setBalance(overview.getBalance().add(transactionLine.getCreditAmount()).subtract(transactionLine.getDebitAmount()));
                }

                overview.setName(transactionLine.getAccount().getName());
                overview.setNumber(transactionLine.getAccount().getNumber());
            }
        }

        return new ArrayList<>(overviewMap.values());

    }
    public String findNextAccountNumberForAccountType(AccountType type){
        String accountNumber = this.repo.findMaxAccountNumberForAccountType(type);
        if(accountNumber != null && !accountNumber.equals("")){
            return String.format("%s", Integer.parseInt(accountNumber) + 1);
        }

        return switch (type){
            case ASSETS -> "100";
            case LIABILITIES -> "200";
            case EQUITY -> "300";
            case REVENUE -> "400";
            case EXPENSES -> "500";
        };
    }

    public List<AccountDTO> findAccountsByNameAndType(String queryName, AccountType accountType){
        return this.repo.findByNameIgnoreCaseContainingAndAccountTypeIs(queryName, accountType).stream().map(mapper::accountToDto).collect(Collectors.toList());
    }

    public List<AccountDTO> findAccountsByName(String queryName){
        return this.repo.findByNameIgnoreCaseContaining(queryName).stream().map(mapper::accountToDto).collect(Collectors.toList());
    }
}
