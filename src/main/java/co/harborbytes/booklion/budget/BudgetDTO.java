package co.harborbytes.booklion.budget;

import co.harborbytes.booklion.account.AccountDTO;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
public class BudgetDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    @Size(min=2, max = 128)
    private String description;

    private Long id;
    private AccountDTO account;

    public void setAmount(BigDecimal amount){
        this.amount = amount.setScale(2, RoundingMode.DOWN);
    }

}
