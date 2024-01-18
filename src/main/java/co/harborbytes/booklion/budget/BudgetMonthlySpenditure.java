package co.harborbytes.booklion.budget;

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
    private BigDecimal spentAmount;
}
