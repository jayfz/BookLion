package co.harborbytes.booklion.budget;

import co.harborbytes.booklion.account.Account;
import co.harborbytes.booklion.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name ="budget", uniqueConstraints = {
        @UniqueConstraint( name = "budget_account_id_unique", columnNames = "account_id")
})

public class Budget {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotNull
    @Positive
    @Column(name = "amount", nullable = false, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Size(min = 2, max = 128)
    @Column(name = "description", length = 128, nullable = false)
    private String description;

    @NotNull
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "account_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "budget_account_id_fk"
            ),
            updatable = false,
            nullable = false
    )
    private Account account;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "budget_user_id_fk"
            ),
            updatable = false,
            nullable = false
    )

    private User user;
}