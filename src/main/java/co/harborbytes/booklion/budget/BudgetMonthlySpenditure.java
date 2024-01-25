package co.harborbytes.booklion.budget;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetMonthlySpenditure {
    private String month;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal spentAmount;
}
