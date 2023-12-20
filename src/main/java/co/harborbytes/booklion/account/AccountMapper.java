package co.harborbytes.booklion.account;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountDTO accountToDto(Account source);

    @Mapping( target = "accountType", ignore = true)
    Account dtoToAccount(AccountDTO source);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping( target = "id", ignore = true)
    @Mapping( target = "number", ignore = true)
    @Mapping( target = "accountType", ignore = true)
    void updateAccountFromDto(AccountDTO dto, @MappingTarget Account account);

}
