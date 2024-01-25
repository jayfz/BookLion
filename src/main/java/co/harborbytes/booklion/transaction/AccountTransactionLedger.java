package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.account.AccountType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal debits;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal credits;
    private Long transactionId;
}


