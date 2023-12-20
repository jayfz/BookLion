package co.harborbytes.booklion.account;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "account", uniqueConstraints = {
        @UniqueConstraint(name = "account_number_unique", columnNames = "number"),
        @UniqueConstraint(name = "account_name_unique", columnNames = "name"),
})
public class Account {

    @Transient
    static final String ACCOUNT_NUMBER_ERROR = "must be composed by 3 digits, and the first one must start with a number ranging from 1 to 5 inclusive";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="id", updatable = false, nullable = false)
    private Long id;

    @NotNull
    @Pattern(regexp = "^[12345]\\d{2}$", message = ACCOUNT_NUMBER_ERROR)
    @Column(
            name = "number",
            length = 3,
            updatable = false,
            nullable = false
    )
    private String number;


    @NotNull
    @Size(min=2, max = 128)
    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type", updatable = false, nullable = false)
    private AccountType accountType;

    public void setNumber(final String number){
        this.number = number;
        this.accountType = findAccountType(number);
    }

    private static AccountType findAccountType(String accountNumber){

        if(accountNumber.startsWith("1"))
            return AccountType.ASSETS;
        if(accountNumber.startsWith("2"))
            return AccountType.LIABILITIES;
        if(accountNumber.startsWith("3"))
            return AccountType.EQUITY;
        if(accountNumber.startsWith("4"))
            return AccountType.REVENUE;
        if(accountNumber.startsWith("5"))
            return AccountType.EXPENSES;

        throw new RuntimeException(String.format("The account type could not be determined by the account number [%s] (it doesn't start with 1,2,3,4,5 or 6", accountNumber));
    }

}
