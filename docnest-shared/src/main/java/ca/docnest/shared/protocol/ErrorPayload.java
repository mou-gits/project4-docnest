package ca.docnest.shared.protocol;

public class ErrorPayload {

    private int code;
    private String message;
    private String details;

    public ErrorPayload() {}

    public ErrorPayload(int code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }
}
