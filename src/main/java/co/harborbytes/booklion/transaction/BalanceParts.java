package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.account.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class BalanceParts {
    private String name;
    private AccountType accountType;
    private BigDecimal debits;
    private BigDecimal credits;
}