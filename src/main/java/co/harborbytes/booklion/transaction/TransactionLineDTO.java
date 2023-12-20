package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.account.AccountDTO;
import co.harborbytes.booklion.transaction.validation.TransactionLineBothAmountsSetConstraint;
import co.harborbytes.booklion.transaction.validation.TransactionLineBothAmountsSetWithZeroConstraint;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@TransactionLineBothAmountsSetWithZeroConstraint
@TransactionLineBothAmountsSetConstraint
public class TransactionLineDTO {

    @NotNull
    @PositiveOrZero
    private BigDecimal debitAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal creditAmount;

    @NotNull
    @Positive
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long accountId;

    private AccountDTO account;

    public void setDebitAmount(BigDecimal amount){
        this.debitAmount = amount.setScale(2, RoundingMode.DOWN);
    }

    public void setCreditAmount(BigDecimal amount){
        this.creditAmount = amount.setScale(2, RoundingMode.DOWN);
    }
}
