package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.account.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementReport {
    private List<AccountStatus> revenue;
    private List<AccountStatus> expenses;
}
