package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.account.Account;
import co.harborbytes.booklion.transaction.validation.TransactionLineBothAmountsSetWithZeroConstraint;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transaction_line")

@TransactionLineBothAmountsSetWithZeroConstraint
public class TransactionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotNull
    @PositiveOrZero
    @Column(name = "debit_amount", updatable = false, nullable = false, scale = 2)
    private BigDecimal debitAmount;

    @NotNull
    @PositiveOrZero
    @Column(name = "credit_amount", updatable = false, nullable = false, scale = 2)
    private BigDecimal creditAmount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "transaction_id",
            foreignKey = @ForeignKey(
                    name = "transaction_line_transaction_id_fk"
            ),
            referencedColumnName = "id",
            nullable = false,
            updatable = false
    )

    private Transaction transaction;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH})
    @JoinColumn(
            name = "account_id",
            foreignKey = @ForeignKey(
                    name = "transaction_line_account_id_fk"
            ),
            referencedColumnName = "id",
            updatable = false,
            nullable = false
    )
    private Account account;
}
