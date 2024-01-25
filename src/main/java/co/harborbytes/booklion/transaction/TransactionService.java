package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.account.Account;
import co.harborbytes.booklion.account.AccountOverviewByType;
import co.harborbytes.booklion.account.AccountStatus;
import co.harborbytes.booklion.account.AccountType;
import co.harborbytes.booklion.exception.DomainEntityNotFoundException;
import co.harborbytes.booklion.exception.DomainEntityValidationException;
import co.harborbytes.booklion.exception.TransactionValidationException;
import co.harborbytes.booklion.user.User;
import co.harborbytes.booklion.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final TransactionMapper mapper;
    private final UserRepository userRepo;
    private final Validator validator;
    private final EntityManager entityManager;

    public TransactionService(TransactionRepository transactionRepo, TransactionMapper mapper, UserRepository userRepo, Validator validator, EntityManager entityManager) {
        this.transactionRepo = transactionRepo;
        this.mapper = mapper;
        this.userRepo = userRepo;
        this.validator = validator;
        this.entityManager = entityManager;
    }

    @Transactional
    public TransactionDTO createTransaction(TransactionDTO dto, Long userId) {

        User user = userRepo
                .findById(userId).orElseThrow(() -> new DomainEntityNotFoundException(User.class.getSimpleName(), "id", userId.toString()));

        Transaction transaction = mapper.dtoToTransaction(dto);
        transaction.setId(null);
        transaction.setUser(user);
//        transaction.setCreatedAt(Instant.now());
        transaction.getLines().forEach(line -> line.setTransaction(transaction));

        if (!transaction.isValid())
            throw new TransactionValidationException("Transaction is invalid. Please check that the accounts in the transaction lines are unique," +
                    "that debits and credits are not both zero simultaneously," +
                    "that they are not both set simultaneously," +
                    " and that the transaction is balanced");

        transactionRepo.save(transaction);
        entityManager.refresh(transaction);
        return mapper.transactionToDto(transaction);
    }


    public Page<TransactionDTO> getTransactionsByUserId(Long userId, Pageable pageable) {
        return transactionRepo
                .findTransactionsByUserId(userId, pageable)
                .map(mapper::transactionToDto);
    }

    public TransactionDTO getTransactionByIdAndUserId(Long id, Long userId) {

        Transaction transaction = transactionRepo
                .findTransactionByIdAndUserId(id, userId).orElseThrow(() -> new DomainEntityNotFoundException(Transaction.class.getSimpleName(), "id", id.toString()));

        return mapper.transactionToDto(transaction);
    }

    public TransactionDTO getTransactionById(Long id) {
        Transaction transactionToFind = transactionRepo
                .findById(id).orElseThrow(() -> new DomainEntityNotFoundException(Transaction.class.getSimpleName(), "id", id.toString()));

        return mapper.transactionToDto(transactionToFind);
    }

    @Transactional
    public TransactionDTO updateTransactionDescription(Long id, Map<String, Object> incompleteTransaction) {


        Transaction transactionToUpdate = transactionRepo
                .findById(id).orElseThrow(() -> new DomainEntityNotFoundException(Transaction.class.getSimpleName(), "id", id.toString()));

        if (!incompleteTransaction.keySet().contains("description"))
            return mapper.transactionToDto(transactionToUpdate);

        Object description = incompleteTransaction.get("description");
        if (description == null)
            transactionToUpdate.setDescription(null);
        else
            transactionToUpdate.setDescription(description.toString());

        BindingResult result = new BeanPropertyBindingResult(transactionToUpdate, "transactionToUpdate");

        validator.validate(transactionToUpdate, result);

        if (result.hasErrors()) {
            throw new DomainEntityValidationException(result);
        }

        return mapper.transactionToDto(transactionToUpdate);
    }

    @Transactional
    public void deleteTransactionsByUserId(Long id) {
        transactionRepo.deleteAllByUserId(id);
    }

    @Transactional
    public void deleteTransactionByIdAndUserId(Long id, Long userId) {
        transactionRepo.deleteTransactionByIdAndUserId(id, userId);
    }

    public List<AccountTransactionLedger> findTransactionsByUserIdAndAccountNumber(Long userId, String accountNumber) {
        return transactionRepo.findTransactionsByUserIdAndAccountNumber(userId, accountNumber);
    }


    public BalanceSheetReport getBalanceSheetReport(Long userId, Instant from) {
        BalanceSheetReport report = new BalanceSheetReport();
        List<Transaction> transactionList = transactionRepo.findAllTransactionsByUserIdAndCreatedAtAfter(userId, from);


        Map<String, AccountStatus> balanceMap = new HashMap<>();

        for (Transaction transaction : transactionList) {
            for (TransactionLine transactionLine : transaction.getLines()) {


                String accountNumber = transactionLine.getAccount().getNumber();
                AccountStatus accountStatus = balanceMap.get(accountNumber);

                if (accountStatus == null) {
                    accountStatus = new AccountStatus();
                    accountStatus.setBalance(new BigDecimal("0.00"));
                    accountStatus.setName(transactionLine.getAccount().getName());
                    balanceMap.put(accountNumber, accountStatus);
                }


                AccountType accountType = transactionLine.getAccount().getAccountType();

                if (accountType == AccountType.ASSETS || accountType == AccountType.EXPENSES) {
                    accountStatus.setBalance(accountStatus.getBalance().add(transactionLine.getDebitAmount()).subtract(transactionLine.getCreditAmount()));
                }

                if (accountType == AccountType.LIABILITIES || accountType == AccountType.EQUITY || accountType == AccountType.REVENUE) {
                    accountStatus.setBalance(accountStatus.getBalance().add(transactionLine.getCreditAmount()).subtract(transactionLine.getDebitAmount()));
                }
            }
        }

        List<AccountStatus> assets = balanceMap.keySet().stream().filter(key -> key.startsWith("1")).map(key -> balanceMap.get(key)).collect(Collectors.toList());
        List<AccountStatus> liabilities = balanceMap.keySet().stream().filter(key -> key.startsWith("2")).map(key -> balanceMap.get(key)).collect(Collectors.toList());
        List<AccountStatus> equity = balanceMap.keySet().stream().filter(key -> key.startsWith("3")).map(key -> balanceMap.get(key)).collect(Collectors.toList());

        report.setAssets(assets);
        report.setLiabilities(liabilities);
        report.setEquity(equity);

        return report;
    }

    public BalanceSheetReport getBalanceSheetReportV2(Long userId, Instant from) {
        List<BalanceParts> accountSummary = transactionRepo.queryAccountSummary(userId, List.of(AccountType.ASSETS, AccountType.LIABILITIES, AccountType.EQUITY));
        BalanceSheetReport balanceSheetReport = new BalanceSheetReport();

        Map<AccountType,  List<AccountStatus>> balanceMap =  accountSummary.stream().collect(Collectors.groupingBy(
                item -> item.getAccountType(),
                Collectors.mapping(item -> {

                    BigDecimal balance;
                    if(item.getCredits() != null && item.getDebits() != null)
                        balance = item.getAccountType().equals(AccountType.ASSETS) ? item.getDebits().subtract(item.getCredits()) : item.getCredits().subtract(item.getDebits());
                    else
                        balance = new BigDecimal("0.00");

                    return new AccountStatus(item.getName(), balance);
                }, Collectors.toList())
        ));

//        List<AccountStatus> assets = accountSummary.stream().filter(summary -> summary.getAccountType().equals(AccountType.ASSETS)).map(summary -> new AccountStatus(summary.getName(), summary.getDebits().subtract(summary.getCredits()))).collect(Collectors.toList());
//        List<AccountStatus> liabilities = accountSummary.stream().filter(summary -> summary.getAccountType().equals(AccountType.LIABILITIES)).map(summary -> new AccountStatus(summary.getName(), summary.getCredits().subtract(summary.getDebits()))).collect(Collectors.toList());
//        List<AccountStatus> equity = accountSummary.stream().filter(summary -> summary.getAccountType().equals(AccountType.EQUITY)).map(summary -> new AccountStatus(summary.getName(), summary.getCredits().subtract(summary.getDebits()))).collect(Collectors.toList());

//        balanceSheetReport.setAssets(assets);
//        balanceSheetReport.setLiabilities(liabilities);
//        balanceSheetReport.setEquity(equity);

        balanceSheetReport.setAssets(balanceMap.get(AccountType.ASSETS));
        balanceSheetReport.setLiabilities(balanceMap.get(AccountType.LIABILITIES));
        balanceSheetReport.setEquity(balanceMap.get(AccountType.EQUITY));

        return balanceSheetReport;
    }

    public IncomeStatementReport getIncomeStatementReport(Long userId, Instant from) {
        IncomeStatementReport report = new IncomeStatementReport();
        List<Transaction> transactionList = transactionRepo.findAllTransactionsByUserIdAndCreatedAtAfter(userId, from);

        Map<String, AccountStatus> balanceMap = new HashMap<>();

        for (Transaction transaction : transactionList) {
            for (TransactionLine transactionLine : transaction.getLines()) {

                String accountNumber = transactionLine.getAccount().getNumber();
                AccountStatus accountStatus = balanceMap.get(accountNumber);

                if (accountStatus == null) {
                    accountStatus = new AccountStatus();
                    accountStatus.setBalance(new BigDecimal("0.00"));
                    accountStatus.setName(transactionLine.getAccount().getName());
                    balanceMap.put(accountNumber, accountStatus);
                }


                AccountType accountType = transactionLine.getAccount().getAccountType();

                if (accountType == AccountType.ASSETS || accountType == AccountType.EXPENSES) {
                    accountStatus.setBalance(accountStatus.getBalance().add(transactionLine.getDebitAmount()).subtract(transactionLine.getCreditAmount()));
                }

                if (accountType == AccountType.LIABILITIES || accountType == AccountType.EQUITY || accountType == AccountType.REVENUE) {
                    accountStatus.setBalance(accountStatus.getBalance().add(transactionLine.getCreditAmount()).subtract(transactionLine.getDebitAmount()));
                }
            }
        }

        List<AccountStatus> revenue = balanceMap.keySet().stream().filter(key -> key.startsWith("4")).map(key -> balanceMap.get(key)).collect(Collectors.toList());
        List<AccountStatus> expenses = balanceMap.keySet().stream().filter(key -> key.startsWith("5")).map(key -> balanceMap.get(key)).collect(Collectors.toList());

        report.setRevenue(revenue);
        report.setExpenses(expenses);

        return report;
    }

    public IncomeStatementReport getIncomeStatementReportV2(Long userId, Instant from){
        List<BalanceParts> accountSummary = transactionRepo.queryAccountSummary(userId, List.of(AccountType.REVENUE, AccountType.EXPENSES));
        IncomeStatementReport incomeStatementReport = new IncomeStatementReport();

        Map<AccountType,  List<AccountStatus>> balanceMap =  accountSummary.stream().collect(Collectors.groupingBy(
                item -> item.getAccountType(),
                Collectors.mapping(item -> {

                    BigDecimal balance;
                    if(item.getCredits() != null && item.getDebits() != null)
                        balance = item.getAccountType().equals(AccountType.EXPENSES) ? item.getDebits().subtract(item.getCredits()) : item.getCredits().subtract(item.getDebits());
                    else
                        balance = new BigDecimal("0.00");

                    return new AccountStatus(item.getName(), balance);
                }, Collectors.toList())
        ));

        incomeStatementReport.setExpenses(balanceMap.get(AccountType.EXPENSES));
        incomeStatementReport.setRevenue(balanceMap.get(AccountType.REVENUE));

        return incomeStatementReport;
    }


}
