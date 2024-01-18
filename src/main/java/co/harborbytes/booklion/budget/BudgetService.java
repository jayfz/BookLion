package co.harborbytes.booklion.budget;

import co.harborbytes.booklion.account.Account;
import co.harborbytes.booklion.account.AccountRepository;
import co.harborbytes.booklion.exception.DomainEntityNotFoundException;
import co.harborbytes.booklion.exception.DomainEntityValidationException;
import co.harborbytes.booklion.transaction.AccountTransactionLedger;
import co.harborbytes.booklion.transaction.Transaction;
import co.harborbytes.booklion.transaction.TransactionRepository;
import co.harborbytes.booklion.user.User;
import co.harborbytes.booklion.user.UserRepository;
import jakarta.persistence.Column;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BudgetService {

    private final BudgetMapper mapper;
    private final BudgetRepository budgetRepo;
    private final AccountRepository accountRepo;
    private final UserRepository userRepo;
    private final TransactionRepository transactionRepository;

    private final Validator validator;
    @Autowired
    public BudgetService(BudgetMapper mapper, BudgetRepository budgetRepo, AccountRepository accountRepo, UserRepository userRepo, Validator validator, TransactionRepository transactionRepository) {
        this.mapper = mapper;
        this.accountRepo = accountRepo;
        this.budgetRepo = budgetRepo;
        this.userRepo = userRepo;
        this.validator = validator;
        this.transactionRepository = transactionRepository;
    }


    @Transactional
//    public BudgetDTO createBudget(BudgetDTO budget, Long accountId, Long userId) {
//
//        User user = userRepo
//                .findById(userId).orElseThrow(() -> new DomainEntityNotFoundException(User.class.getSimpleName(), "id", userId.toString()));
//
//        Account account = accountRepo
//                .findById(accountId).orElseThrow(() -> new DomainEntityNotFoundException(Account.class.getSimpleName(), "id", accountId.toString()));
//
//
//        Budget mappedBudget = mapper.dtoToBudget(budget);
//        mappedBudget.setId(null);
//        mappedBudget.setAccount(account);
//        mappedBudget.setUser(user);
//        return mapper.budgetToDto(budgetRepo.save(mappedBudget));
//    }
    public BudgetWithSpendingOverTimeDTO createBudget(CreateBudgetDTO budget, Long accountId, Long userId) {

        User user = userRepo
                .findById(userId).orElseThrow(() -> new DomainEntityNotFoundException(User.class.getSimpleName(), "id", userId.toString()));

        Account account = accountRepo
                .findById(accountId).orElseThrow(() -> new DomainEntityNotFoundException(Account.class.getSimpleName(), "id", accountId.toString()));


        Budget mappedBudget = mapper.createBudgetDtoToBudget(budget);
        mappedBudget.setId(null);
        mappedBudget.setAccount(account);
        mappedBudget.setUser(user);

        BudgetWithSpendingOverTimeDTO budgetWithSpendingOverTimeDTO = mapper.budgetToBudgetWithSpendingOverTimeDTO(budgetRepo.save(mappedBudget));
        budgetWithSpendingOverTimeDTO.setSpending(computeBudgetSpenditureUpUntilDate(userId, account.getNumber(), Instant.now()));
        return budgetWithSpendingOverTimeDTO;
    }

    public BudgetWithSpendingOverTimeDTO findBudgetById(Long id, Long userId) {
        Budget budgetToFind = budgetRepo
                .findByIdAndUserId(id, userId).orElseThrow(() -> new DomainEntityNotFoundException(Budget.class.getSimpleName(), "id", id.toString()));

        BudgetWithSpendingOverTimeDTO budgetWithSpendingOverTimeDTO = mapper.budgetToBudgetWithSpendingOverTimeDTO(budgetToFind);
        budgetWithSpendingOverTimeDTO.setSpending(computeBudgetSpenditureUpUntilDate(userId, budgetToFind.getAccount().getNumber(), Instant.now()));
        return budgetWithSpendingOverTimeDTO;
    }

    public Page<ReadBudgetDTO> findBudgetsByUserId(Long userId, Pageable pageable) {


        Page<ReadBudgetDTO> readBudgetDTOPage = budgetRepo.findAllByUserId(userId, pageable)
                .map(mapper::budgetToReadBudgetDto);

        readBudgetDTOPage.forEach(readBudgetDTO -> {
            readBudgetDTO.setSpentSoFar(computeBudgetSpenditureForCurrentMonth(userId, readBudgetDTO.getAccountNumber()));
        });

        return readBudgetDTOPage;
    }

    public BigDecimal computeBudgetSpenditureForCurrentMonth(Long userId, String accountNumber){


        Instant startDate = Instant.parse(String.format("%sT00:00:00Z", YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atDay(1)));
        Instant endDate = Instant.parse(String.format("%sT00:00:00Z", YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atEndOfMonth()));
        List<AccountTransactionLedger> transactions = transactionRepository.findTransactionsByUserIdAndAccountNumberBetweenDates(userId, accountNumber,startDate, endDate);

        BigDecimal spentSoFar =  new BigDecimal("0.00");
        for(AccountTransactionLedger transaction : transactions){
            spentSoFar = spentSoFar.add(transaction.getDebits().subtract(transaction.getCredits()));
        }
        return spentSoFar;
    }

    public List<BudgetMonthlySpenditure> computeBudgetSpenditureUpUntilDate(Long userId, String accountNumber, Instant to){
        return computeBudgetSpenditureBetweenDates(userId, accountNumber, Instant.parse("2000-01-01T00:00:00Z"), to);
    }

    public List<BudgetMonthlySpenditure> computeBudgetSpenditureBetweenDates(Long userId, String accountNumber, Instant from, Instant to){


        List<AccountTransactionLedger> transactions = transactionRepository.findTransactionsByUserIdAndAccountNumberBetweenDates(userId, accountNumber,from, to);
        Map<String, BigDecimal> spenditureOverTime = new HashMap<>();

        for(AccountTransactionLedger transaction : transactions){

            String period = getMonthAndYear(transaction.getDate());
            BigDecimal spentOnPeriod = spenditureOverTime.get(period);
            if(spentOnPeriod == null){
                spentOnPeriod = new BigDecimal("0.00");
            }

            spenditureOverTime.put(period, spentOnPeriod.add(transaction.getDebits().subtract(transaction.getCredits())));
        }

        List<BudgetMonthlySpenditure> monthlySpenditures = new ArrayList<>();
        spenditureOverTime.forEach((month, spentAmount) ->{
            monthlySpenditures.add(new BudgetMonthlySpenditure(month, spentAmount));
        });

        return monthlySpenditures;
    }

    public String getMonthAndYear(Instant instant){
        ZoneId z = ZoneId.of("UTC");
        ZonedDateTime zdt = ZonedDateTime.now(z);
        return String.format("%s-%02d", zdt.getYear(), zdt.getMonthValue());
    }


//    @Transactional
//    public BudgetDTO updateBudget(BudgetDTO budgetDto, Long accountId, Long userId, AtomicBoolean didCreate) {
//
//        if (budgetDto.getId() == null)
//            throw new RuntimeException("Budget id can not be null. If you know the Id is already null, you must use the create endpoint");
//
//        Account account = accountRepo
//                .findById(accountId).orElseThrow(() -> new DomainEntityNotFoundException(Account.class.getSimpleName(), "id", accountId.toString()));
//
//        User user = userRepo
//                .findById(userId).orElseThrow(() -> new DomainEntityNotFoundException(User.class.getSimpleName(), "id", userId.toString()));
//
//        Optional<Budget> maybeBudget = budgetRepo.findById(budgetDto.getId());
//
//        Budget budget;
//        if (maybeBudget.isEmpty()) {
//            Budget mappedBudget = mapper.dtoToBudget(budgetDto);
//            mappedBudget.setId(null);
//            mappedBudget.setAccount(account);
//            mappedBudget.setUser(user);
//            budget = budgetRepo.save(mappedBudget);
//            didCreate.set(true);
//        } else {
//            budget = maybeBudget.get();
//            mapper.updateBudgetFromDto(budgetDto, budget);
//            didCreate.set(false);
//        }
//
//        return mapper.budgetToDto(budget);
//    }

    @Transactional
    public BudgetWithSpendingOverTimeDTO partiallyUpdateBudget(Map<String, Object> incompleteBudget, Long id, Long userId) {


        Budget budgetToUpdate = budgetRepo
                .findByIdAndUserId(id, userId).orElseThrow(() -> new DomainEntityNotFoundException(Budget.class.getSimpleName(), "id", id.toString()));


        Field[] budgetFields = Budget.class.getDeclaredFields();
        BindingResult result = new BeanPropertyBindingResult(budgetToUpdate, "budgetToUpdate");

        for (Field budgetField : budgetFields) {

            if(!incompleteBudget.keySet().contains(budgetField.getName()))
                continue;

            if (Modifier.isStatic(budgetField.getModifiers()))
                continue;

            if (budgetField.getName().equals("id"))
                continue;

            Column column = budgetField.getAnnotation(Column.class);

            if(column == null)
                continue;

            if (!column.updatable())
                continue;


            Object jsonValue = incompleteBudget.get(budgetField.getName());

            budgetField.setAccessible(true);
            try {

                if(budgetField.getType().equals(BigDecimal.class)){
                    if(jsonValue == null) throw new IllegalArgumentException();
                    BigDecimal amount = new BigDecimal(jsonValue.toString());
                    budgetField.set(budgetToUpdate, amount);
                }
                else{
                    budgetField.set(budgetToUpdate, jsonValue);

                }

            } catch (IllegalAccessException | IllegalArgumentException e) {
                result.addError(
                        new FieldError(
                                "budgetToUpdate",
                                budgetField.getName(),
                                String.format("field cannot be set with  \"%s\"", jsonValue)));
            }

        }

//        if (result.hasErrors())
//            throw new DomainEntityValidationException(result);

        validator.validate(budgetToUpdate, result);
        if (result.hasErrors())
            throw new DomainEntityValidationException(result);

        BudgetWithSpendingOverTimeDTO budgetWithSpendingOverTimeDTO = mapper.budgetToBudgetWithSpendingOverTimeDTO(budgetToUpdate);
        budgetWithSpendingOverTimeDTO.setSpending(computeBudgetSpenditureUpUntilDate(userId, budgetToUpdate.getAccount().getNumber(), Instant.now()));
//        return mapper.budgetToDto(budgetToUpdate);

        return budgetWithSpendingOverTimeDTO;
    }

    @Transactional
    public void deleteBudget(Long id, Long userId) {
        budgetRepo.deleteByIdAndUserId(id, userId);
    }


}
