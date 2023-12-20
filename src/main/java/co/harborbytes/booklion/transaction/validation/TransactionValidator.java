package co.harborbytes.booklion.transaction.validation;

import co.harborbytes.booklion.transaction.Transaction;
import co.harborbytes.booklion.transaction.TransactionDTO;
import co.harborbytes.booklion.transaction.TransactionLineDTO;
import co.harborbytes.booklion.transaction.TransactionMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionValidator implements ConstraintValidator<TransactionConstraint, List<TransactionLineDTO>> {
    private TransactionMapper mapper;

    @Autowired
    public TransactionValidator(TransactionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isValid(List<TransactionLineDTO> linesList, ConstraintValidatorContext constraintValidatorContext) {

        BigDecimal zero = new BigDecimal("0.00");
//        TransactionDTO dto = null;
//
//        if (linesList instanceof TransactionDTO tsdto)
//            dto = tsdto;
//
//        if (object instanceof Transaction ts)
//            dto = mapper.transactionToDto(ts);


        if (linesList == null)
            return false;

        boolean hasDuplicatedAccounts = linesList.stream()
                .map(line -> line.getAccountId()).collect(Collectors.toSet()).size() != linesList.size();

        BigDecimal balance = linesList.stream()
                .map(line -> line.getDebitAmount().subtract(line.getCreditAmount())).reduce(zero, (acc, amount) -> acc.add(amount));

        boolean isTransactionUnbalanced = !balance.equals(zero);

        return !hasDuplicatedAccounts && !isTransactionUnbalanced;
    }

    @Override
    public void initialize(TransactionConstraint constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }
}
