package co.harborbytes.booklion.apiresponsewrapper;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.domain.*;

import java.util.List;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseSuccess<T> {

    private final String status = "success";
    private T data;
    private PageSummary page;

    public ApiResponseSuccess(T data) {
        this.data = data;
    }

    public ApiResponseSuccess(Page pageable) {
        this.data = (T) pageable.getContent();
        this.page = new PageSummary(pageable.getTotalElements(), pageable.getTotalPages(), pageable.isFirst(), pageable.isLast(),pageable.getSort().toString(), pageable.getNumber());

    }
    private record PageSummary(long totalElements, long totalPages, boolean first, boolean last, String order, int number) {}
}


