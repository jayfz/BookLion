package co.harborbytes.booklion.apiresponsewrapper;

import lombok.Data;

import java.time.Instant;

@Data
public class ApiResponseError {
    private final String status = "error";
    private String message;
    private Instant timestamp;
    private int code;
    private String debug;
}
