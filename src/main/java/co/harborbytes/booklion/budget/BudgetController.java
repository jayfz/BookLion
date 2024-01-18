package co.harborbytes.booklion.budget;

import co.harborbytes.booklion.apiresponsewrapper.ApiResponseSuccess;
import co.harborbytes.booklion.exception.DomainEntityValidationException;
import co.harborbytes.booklion.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


@RestController

public class BudgetController {

    private final BudgetService budgetService;

    @Autowired
    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("/accounts/{id}/budget")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseSuccess<BudgetWithSpendingOverTimeDTO> createBudget(@PathVariable("id") @Positive Long accountId, @Validated @RequestBody CreateBudgetDTO createBudgetDTO, BindingResult result) {

        if(result.hasErrors()){
            throw new DomainEntityValidationException(result);
        }
        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return new ApiResponseSuccess<>(
                budgetService.createBudget(createBudgetDTO, accountId, loggedInUser.getId())
        );
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/budgets/{id}")
    public ApiResponseSuccess<BudgetWithSpendingOverTimeDTO> getBudgetById(@PathVariable("id") @Positive Long id) {

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return new ApiResponseSuccess<>(
                budgetService.findBudgetById(id, loggedInUser.getId())
        );
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/budgets")

    public ApiResponseSuccess<Page<ReadBudgetDTO>> getBudgets(@PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return new ApiResponseSuccess<>(
                budgetService.findBudgetsByUserId(loggedInUser.getId(), pageable)
        );
    }

//    @PutMapping("/accounts/{id}/budget/{budgetId}")
//    public ResponseEntity<ApiResponseSuccess<BudgetDTO>> updateBudget(@PathVariable("id") @Positive Long accountId , @PathVariable("budgetId") @Positive Long budgetId, @Validated @RequestBody BudgetDTO budgetDto, BindingResult result) {
//
//        if(result.hasErrors()){
//            throw new DomainEntityValidationException(result);
//        }
//
//        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//
//        budgetDto.setId(budgetId);
//        AtomicBoolean didCreate = new AtomicBoolean(false);
//        BudgetDTO updatedBudget = budgetService.updateBudget(budgetDto, accountId, loggedInUser.getId(), didCreate);
//
//        return new ResponseEntity<>(
//                new ApiResponseSuccess<>(updatedBudget),
//                didCreate.get() ? HttpStatus.CREATED : HttpStatus.OK
//        );
//
//    }

    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/budgets/{id}")
    public ApiResponseSuccess<BudgetWithSpendingOverTimeDTO> partiallyUpdateBudget(@PathVariable("id") @Positive Long id, @RequestBody Map<String, Object> incompleteBudget) {

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return new ApiResponseSuccess<>(
                budgetService.partiallyUpdateBudget(incompleteBudget, id, loggedInUser.getId())
        );
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/budgets/{id}")
    public ApiResponseSuccess<String> deleteBudgetById(@PathVariable("id") @Positive Long id) {

        User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        budgetService.deleteBudget(id, loggedInUser.getId());
        return new ApiResponseSuccess<>("");
    }
}
