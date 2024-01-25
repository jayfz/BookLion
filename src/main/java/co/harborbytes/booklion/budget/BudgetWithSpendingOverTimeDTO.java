package co.harborbytes.booklion.budget;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetWithSpendingOverTimeDTO {
    private Long budgetId;
    private String accountNumber;
    private String name;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amount;
//    private Map<String, BigDecimal> spending;
    private List<BudgetMonthlySpenditure> spending;
}
