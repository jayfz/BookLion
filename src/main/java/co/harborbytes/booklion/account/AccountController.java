package co.harborbytes.booklion.account;

import co.harborbytes.booklion.apiresponsewrapper.ApiResponseFail;
import co.harborbytes.booklion.apiresponsewrapper.ApiResponseSuccess;
import co.harborbytes.booklion.budget.BudgetDTO;
import co.harborbytes.booklion.budget.BudgetService;
import co.harborbytes.booklion.exception.DomainEntityNotFoundException;
import co.harborbytes.booklion.exception.DomainEntityValidationException;
import co.harborbytes.booklion.user.User;
import co.harborbytes.booklion.user.UserRepository;
import com.github.javafaker.Faker;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static co.harborbytes.booklion.account.Account.ACCOUNT_NUMBER_ERROR;


@RestController
@RequestMapping( "/accounts")
public class AccountController {

    private final AccountService accountService;


    @Autowired
    public AccountController(AccountService accountService){
        this.accountService = accountService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseSuccess<AccountDTO> createAccount(@Valid @RequestBody AccountDTO accountToCreate, BindingResult result) {

        if (result.hasErrors()){
            throw new DomainEntityValidationException(result);
        }

        AccountDTO createdAccount = accountService.createAccount(accountToCreate);
        return new ApiResponseSuccess<>(createdAccount);
    }


    @GetMapping( "/{id}" )
    @ResponseStatus(HttpStatus.OK)
    public ApiResponseSuccess<AccountDTO> getAccountById(@PathVariable("id") @Positive Long id) {
        return new ApiResponseSuccess<>(accountService.findAccountById(id));
    }

    @GetMapping( params = "accountNumber")
    @ResponseStatus(HttpStatus.OK)

    public ApiResponseSuccess<AccountDTO> getAccountByNumber(@RequestParam(value = "accountNumber") @Pattern(regexp = "^[12345]\\d{2}$", message = ACCOUNT_NUMBER_ERROR) String accountNumber) {
        return new ApiResponseSuccess<>(accountService.findAccountByNumber(accountNumber));
    }
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponseSuccess<List<AccountDTO>> getAccounts(@PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        Page<AccountDTO> result = accountService.findAllAccounts(pageable);
        return new ApiResponseSuccess<>(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseSuccess<AccountDTO>> updateAccount( @PathVariable("id") @Positive Long accountId, @Validated @RequestBody AccountDTO accountToUpdate, BindingResult results) {

        if(results.hasErrors())
            throw new DomainEntityValidationException(results);

        accountToUpdate.setId(accountId);
        AtomicBoolean didCreate = new AtomicBoolean(false);
        ApiResponseSuccess<AccountDTO> response = new ApiResponseSuccess<>(accountService.updateAccount(accountToUpdate, didCreate));

        HttpStatus status = didCreate.get() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponseSuccess<AccountDTO> partiallyUpdateAccount(@PathVariable("id") @Positive Long id, @RequestBody Map<String, Object> accountToUpdate) {
        return new ApiResponseSuccess<>(accountService.partiallyUpdateAccount(accountToUpdate, id));
    }
}
