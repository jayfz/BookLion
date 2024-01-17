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
public class BalanceSheetReport {

    private List<AccountStatus> assets;
    private List<AccountStatus> liabilities;
    private List<AccountStatus> equity;
}
