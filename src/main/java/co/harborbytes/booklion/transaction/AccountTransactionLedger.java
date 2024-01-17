package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.account.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountTransactionLedger {
    private Instant date;
    private String description;
    private AccountType accountType;
    private BigDecimal debits;
    private BigDecimal credits;
    private Long transactionId;
}


