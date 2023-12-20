package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.account.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class TransactionMapper {

    @Mapping( target = "id", ignore = true)
    @Mapping( target = "createdAt", ignore = true)
    @Mapping( target = "user", ignore = true)
    public abstract Transaction dtoToTransaction (TransactionDTO dto);
    public abstract TransactionDTO transactionToDto (Transaction transaction);

    public  TransactionLine dtoToTransactionLine (TransactionLineDTO dto){
        if ( dto == null ) {
            return null;
        }

        TransactionLine transactionLine = new TransactionLine();
        transactionLine.setDebitAmount( dto.getDebitAmount() );
        transactionLine.setCreditAmount( dto.getCreditAmount() );
        Account account = new Account();
        account.setId(dto.getAccountId());
        transactionLine.setAccount(account);

        return transactionLine;
    }

    @Mapping( target = "accountId", ignore = true)
    public abstract TransactionLineDTO transactionLineToDto (TransactionLine line);

}
