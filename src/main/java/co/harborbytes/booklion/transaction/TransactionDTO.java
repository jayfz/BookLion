package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.transaction.validation.TransactionConstraint;
import co.harborbytes.booklion.user.User;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;


@Getter
@Setter
public class TransactionDTO {

    @NotNull
    @Size(min = 2, max= 128)
    private String description;

    @NotNull
    @Size(min = 2)
    @Valid
    @TransactionConstraint
    private List<TransactionLineDTO> lines;

    private Long id;
    private Instant createdAt;
}
