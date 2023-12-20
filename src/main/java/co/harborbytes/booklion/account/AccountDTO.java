package co.harborbytes.booklion.account;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static co.harborbytes.booklion.account.Account.ACCOUNT_NUMBER_ERROR;


@Getter
@Setter
public class AccountDTO {

    @NotNull
    @Pattern(regexp = "^[12345]\\d{2}$", message = ACCOUNT_NUMBER_ERROR)
    private String number;

    @NotNull
    @Size(min = 2, max = 128)
    private String name;

    private Long id;
    private String accountType;
}

