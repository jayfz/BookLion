package co.harborbytes.booklion.account;
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
public class IndividualAccountOverview {

    private String type;
    private Integer transactionCount;
    private String name;
    private String number;
    private Instant dateLastTransaction;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal balance;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal variation;

    public IndividualAccountOverview() {
        transactionCount = 0;
        balance = new BigDecimal("0.00");
        variation = new BigDecimal("0.00");
    }
}
