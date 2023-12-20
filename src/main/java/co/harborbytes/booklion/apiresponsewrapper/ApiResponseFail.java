package co.harborbytes.booklion.apiresponsewrapper;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class ApiResponseFail<T> {
    private final String status = "fail";
    private Instant timestamp;
    private List<String> errors = new ArrayList<>();
    private String debug;

    public void addMessage(String message){
        this.errors.add(message);
    }
}
