package co.harborbytes.booklion.budget;

import co.harborbytes.booklion.account.AccountDTO;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Getter
@Setter
public class ReadBudgetDTO {

    private Long id;
    private String description;
    private String accountNumber;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amount;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal spentSoFar;
}
