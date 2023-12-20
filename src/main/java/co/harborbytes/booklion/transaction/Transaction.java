package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transaction")

public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotNull
    @PastOrPresent
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @NotNull
    @Size(min = 2, max= 128)
    @Column(name = "description", length = 128, nullable = false)
    private String description;

    @NotNull
    @ManyToOne(
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "transaction_user_id_fk"
            ),
            updatable = false,
            nullable = false
    )
    private User user;

    @NotNull
    @Size(min = 2)
    @OneToMany(
            orphanRemoval = true,
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.REFRESH,
                    CascadeType.REMOVE,
            },
            fetch = FetchType.EAGER,
            mappedBy = "transaction"
    )
//    @TransactionConstraint
    private List<TransactionLine> lines;

    public boolean isValid(){

//        if (lines == null || lines.size() < 2)
//            return false;
//
//        boolean hasDuplicatedAccounts = lines.stream()
//                .map(line -> line.getAccount().getId()).collect(Collectors.toSet()).size() != lines.size();

//        boolean bothCreditsAndDebitsAreSet = lines.stream()
//                .anyMatch(line -> line.getCreditAmount().doubleValue() > 0 && line.getDebitAmount().doubleValue() > 0);
//
//        BigDecimal zero = new BigDecimal("0.00");
//        boolean bothCreditsAndDebitsAreZero = lines.stream()
//                .anyMatch(line -> line.getCreditAmount().equals(zero) && line.getDebitAmount().equals(zero));

//        BigDecimal balance = lines.stream()
//                .map(line -> line.getDebitAmount().subtract(line.getCreditAmount())).reduce(zero, (acc,amount) -> acc.add(amount));
//
//        boolean isTransactionUnbalanced = !balance.equals(zero);
//
//        return false;
//        return !(hasDuplicatedAccounts || bothCreditsAndDebitsAreSet || bothCreditsAndDebitsAreZero || isTransactionUnbalanced);

        return true;

    }

}
