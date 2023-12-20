package co.harborbytes.booklion.budget;

import co.harborbytes.booklion.account.Account;
import co.harborbytes.booklion.account.AccountRepository;
import co.harborbytes.booklion.exception.DomainEntityNotFoundException;
import co.harborbytes.booklion.exception.DomainEntityValidationException;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetMapper mapper;
    private final BudgetRepository budgetRepo;
    private final AccountRepository accountRepo;
    private final UserRepository userRepo;

    private final Validator validator;
    @Autowired
    public BudgetService(BudgetMapper mapper, BudgetRepository budgetRepo, AccountRepository accountRepo, UserRepository userRepo, Validator validator) {
        this.mapper = mapper;
        this.accountRepo = accountRepo;
        this.budgetRepo = budgetRepo;
        this.userRepo = userRepo;
        this.validator = validator;
    }


    @Transactional
    public BudgetDTO createBudget(BudgetDTO budget, Long accountId, Long userId) {

        User user = userRepo
                .findById(userId).orElseThrow(() -> new DomainEntityNotFoundException(User.class.getSimpleName(), "id", userId.toString()));

        Account account = accountRepo
                .findById(accountId).orElseThrow(() -> new DomainEntityNotFoundException(Account.class.getSimpleName(), "id", accountId.toString()));


        Budget mappedBudget = mapper.dtoToBudget(budget);
        mappedBudget.setId(null);
        mappedBudget.setAccount(account);
        mappedBudget.setUser(user);
        return mapper.budgetToDto(budgetRepo.save(mappedBudget));
    }

    public BudgetDTO findBudgetById(Long id, Long userId) {
        Budget budgetToFind = budgetRepo
                .findByIdAndUserId(id, userId).orElseThrow(() -> new DomainEntityNotFoundException(Budget.class.getSimpleName(), "id", id.toString()));

        return mapper.budgetToDto(budgetToFind);
    }

    public Page<BudgetDTO> findBudgetsByUserId(Long userId, Pageable pageable) {
        return budgetRepo.findAllByUserId(userId, pageable)
                .map(mapper::budgetToDto);
    }


    @Transactional
    public BudgetDTO updateBudget(BudgetDTO budgetDto, Long accountId, Long userId, AtomicBoolean didCreate) {

        if (budgetDto.getId() == null)
            throw new RuntimeException("Budget id can not be null. If you know the Id is already null, you must use the create endpoint");

        Account account = accountRepo
                .findById(accountId).orElseThrow(() -> new DomainEntityNotFoundException(Account.class.getSimpleName(), "id", accountId.toString()));

        User user = userRepo
                .findById(userId).orElseThrow(() -> new DomainEntityNotFoundException(User.class.getSimpleName(), "id", userId.toString()));

        Optional<Budget> maybeBudget = budgetRepo.findById(budgetDto.getId());

        Budget budget;
        if (maybeBudget.isEmpty()) {
            Budget mappedBudget = mapper.dtoToBudget(budgetDto);
            mappedBudget.setId(null);
            mappedBudget.setAccount(account);
            mappedBudget.setUser(user);
            budget = budgetRepo.save(mappedBudget);
            didCreate.set(true);
        } else {
            budget = maybeBudget.get();
            mapper.updateBudgetFromDto(budgetDto, budget);
            didCreate.set(false);
        }

        return mapper.budgetToDto(budget);
    }

    @Transactional
    public BudgetDTO partiallyUpdateBudget(Map<String, Object> incompleteBudget, Long id, Long userId) {


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

        return mapper.budgetToDto(budgetToUpdate);

    }

    @Transactional
    public void deleteBudget(Long id, Long userId) {
        budgetRepo.deleteByIdAndUserId(id, userId);
    }

}
