package ca.docnest.shared.protocol;

/**
 * @class ErrorPayload
 * @brief Represents structured error information transferred in the DocNest protocol.
 *
 * @details
 * The {@code ErrorPayload} class is a simple data model used to carry error
 * details between the client and server when an operation fails.
 *
 * It provides a standardized format for communicating failures instead of
 * sending plain text messages. This makes error handling easier to parse,
 * display, and log.
 *
 * The payload contains:
 * <ul>
 *   <li>A numeric error code</li>
 *   <li>A human-readable error message</li>
 *   <li>Optional detailed diagnostic information</li>
 * </ul>
 *
 * Typical usage includes:
 * <ul>
 *   <li>Authentication failures</li>
 *   <li>Protocol violations</li>
 *   <li>Missing files</li>
 *   <li>Transfer errors</li>
 *   <li>Unexpected server exceptions</li>
 * </ul>
 *
 * This class is commonly serialized into JSON and included inside an
 * {@link DataPacket} whose command type is an error-related packet.
 */
public class ErrorPayload {

    /**
     * @brief Numeric error code identifying the failure type.
     *
     * @details
     * Used by the client or server to categorize and process errors
     * programmatically.
     */
    private int code;

    /**
     * @brief Human-readable summary of the error.
     *
     * @details
     * Provides a concise explanation suitable for display to users or logs.
     */
    private String message;

    /**
     * @brief Additional diagnostic details about the error.
     *
     * @details
     * May contain contextual information useful for debugging, tracing, or
     * identifying the source of the problem.
     */
    private String details;

    /**
     * @brief Default constructor.
     *
     * @details
     * Creates an empty {@code ErrorPayload} object with default field values.
     * Commonly required for JSON deserialization frameworks.
     */
    public ErrorPayload() {}

    /**
     * @brief Constructs a fully initialized error payload.
     *
     * @details
     * Creates an error object containing the supplied code, message, and
     * optional details.
     *
     * @param code Numeric error code.
     * @param message Short description of the error.
     * @param details Additional context or debugging details.
     */
    public ErrorPayload(int code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    /**
     * @brief Returns the numeric error code.
     *
     * @return The error code value.
     */
    public int getCode() {
        return code;
    }

    /**
     * @brief Returns the error message.
     *
     * @return The short human-readable error message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @brief Returns additional error details.
     *
     * @return Diagnostic or contextual detail text.
     */
    public String getDetails() {
        return details;
    }
}