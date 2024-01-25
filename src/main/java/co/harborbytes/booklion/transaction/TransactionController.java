package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.apiresponsewrapper.ApiResponseSuccess;
import co.harborbytes.booklion.exception.DomainEntityValidationException;
import co.harborbytes.booklion.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final Instant defaultFromDate = Instant.parse("2024-01-01T00:00:00Z");


    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseSuccess<TransactionDTO> createTransaction(@Valid @RequestBody TransactionDTO transactionDTO, BindingResult result) {

        if(result.hasErrors()){
            throw new DomainEntityValidationException(result);
        }

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        TransactionDTO responsePayload = transactionService.createTransaction(transactionDTO, loggedInUser.getId());

        return new ApiResponseSuccess<>(responsePayload);
    }

    @GetMapping(value = "/balance-sheet", params= "from")
    public ApiResponseSuccess<BalanceSheetReport> getBalanceSheetReport(@RequestParam("from") String fromDate){

        Instant date;
        try{
            date = Instant.parse(fromDate);
        }
        catch (DateTimeParseException dtpe){
            date = defaultFromDate;
        }

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new ApiResponseSuccess<>(transactionService.getBalanceSheetReportV2(loggedInUser.getId(), date) );
    }

    @GetMapping(value = "/income-statement", params= "from")
    public ApiResponseSuccess<IncomeStatementReport> getIncomeStatementReport(@RequestParam("from") String fromDate){

        Instant date;
        try{
            date = Instant.parse(fromDate);
        }
        catch (DateTimeParseException dtpe){
            date = defaultFromDate;
        }

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new ApiResponseSuccess<>(transactionService.getIncomeStatementReportV2(loggedInUser.getId(), date) );
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public ApiResponseSuccess<Page<TransactionDTO>> getTransactionsByUser(@PageableDefault(page = 0, size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable){

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Page<TransactionDTO> responsePayload = transactionService.getTransactionsByUserId(loggedInUser.getId(), pageable);
        return new ApiResponseSuccess<>(responsePayload);
    }

    @GetMapping(value = "/generalLedger", params = "accountNumber")
    public ApiResponseSuccess<List<AccountTransactionLedger>> findTransactionsByUserIdAndAccountNumber(@RequestParam("accountNumber") String accountNumber){
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new ApiResponseSuccess<>(transactionService.findTransactionsByUserIdAndAccountNumber(loggedInUser.getId(), accountNumber)) ;
    }



    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    public ApiResponseSuccess<TransactionDTO> getTransactionById(@PathVariable("id") @Positive  Long id){

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        TransactionDTO responsePayload = transactionService.getTransactionByIdAndUserId(id, loggedInUser.getId());
        return new ApiResponseSuccess<>(responsePayload);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public ApiResponseSuccess<String> deleteTransactionById(@PathVariable("id") @Positive  Long id){

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        transactionService.deleteTransactionByIdAndUserId(id, loggedInUser.getId());
        return new ApiResponseSuccess<>("");
    }
}
