package co.harborbytes.booklion.transaction.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TransactionLineBothAmountsSetValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionLineBothAmountsSetConstraint {

    String message() default "Transaction line must not have both credits and debits set";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
