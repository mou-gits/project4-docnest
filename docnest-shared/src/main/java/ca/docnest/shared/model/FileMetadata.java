package ca.docnest.shared.model;

/**
 * @class FileMetadata
 * @brief Represents descriptive metadata for a file stored in the DocNest system.
 *
 * @details
 * The {@code FileMetadata} class is a data model used to store information
 * about files uploaded to the DocNest platform.
 *
 * It contains identifying, descriptive, ownership, and audit information
 * related to a stored file, including:
 * <ul>
 *   <li>Internal file identifier</li>
 *   <li>Original filename</li>
 *   <li>File size in bytes</li>
 *   <li>File type or MIME type</li>
 *   <li>User who uploaded the file</li>
 *   <li>Upload date and time</li>
 *   <li>Additional descriptive information</li>
 * </ul>
 *
 * This class follows the JavaBean pattern with:
 * <ul>
 *   <li>Private fields</li>
 *   <li>A default constructor</li>
 *   <li>A parameterized constructor</li>
 *   <li>Getter and setter methods</li>
 * </ul>
 *
 * It is commonly used for:
 * <ul>
 *   <li>JSON serialization and deserialization</li>
 *   <li>Displaying file information in the UI</li>
 *   <li>Maintaining metadata records on the server</li>
 *   <li>Transferring file information between client and server</li>
 * </ul>
 */
public class FileMetadata {

    /**
     * @brief Internal unique identifier assigned to the file.
     *
     * @details
     * Used by the system to reference the stored file independently from the
     * original filename.
     */
    private String fileId;

    /**
     * @brief Original name of the uploaded file.
     *
     * @details
     * This is the human-readable filename supplied by the user.
     */
    private String filename;

    /**
     * @brief Size of the file in bytes.
     *
     * @details
     * Represents the total number of bytes contained in the file.
     */
    private long size;

    /**
     * @brief File type or MIME type.
     *
     * @details
     * Used to identify the content format of the file, such as text, image,
     * PDF, or binary data.
     */
    private String type;

    /**
     * @brief Identifier of the user who uploaded the file.
     *
     * @details
     * Stores ownership information for access control and auditing.
     */
    private String uploadedBy;

    /**
     * @brief Date and time when the file was uploaded.
     *
     * @details
     * Stored as a formatted string representing the upload timestamp.
     */
    private String uploadDate;

    /**
     * @brief Additional descriptive information about the file.
     *
     * @details
     * Optional user-provided notes, comments, or contextual details.
     */
    private String additionalInfo;

    /**
     * @brief Default constructor.
     *
     * @details
     * Creates an empty {@code FileMetadata} object with fields initialized to
     * default values. Commonly required for frameworks such as JSON parsers.
     */
    public FileMetadata() {}

    /**
     * @brief Constructs a fully initialized file metadata object.
     *
     * @details
     * Creates a metadata record containing all primary file attributes.
     *
     * @param fileId Internal unique file identifier.
     * @param filename Original filename.
     * @param size File size in bytes.
     * @param type File type or MIME type.
     * @param uploadedBy User who uploaded the file.
     * @param uploadDate Upload timestamp as a formatted string.
     * @param additionalInfo Additional descriptive information.
     */
    public FileMetadata(String fileId,
                        String filename,
                        long size,
                        String type,
                        String uploadedBy,
                        String uploadDate,
                        String additionalInfo) {
        this.fileId = fileId;
        this.filename = filename;
        this.size = size;
        this.type = type;
        this.uploadedBy = uploadedBy;
        this.uploadDate = uploadDate;
        this.additionalInfo = additionalInfo;
    }

    /**
     * @brief Returns the internal file ID.
     *
     * @return The unique file identifier.
     */
    public String getFileId() { return fileId; }

    /**
     * @brief Sets the internal file ID.
     *
     * @param fileId The unique file identifier to assign.
     */
    public void setFileId(String fileId) { this.fileId = fileId; }

    /**
     * @brief Returns the original filename.
     *
     * @return The stored filename.
     */
    public String getFilename() { return filename; }

    /**
     * @brief Sets the original filename.
     *
     * @param filename The filename to assign.
     */
    public void setFilename(String filename) { this.filename = filename; }

    /**
     * @brief Returns the file size in bytes.
     *
     * @return The file size.
     */
    public long getSize() { return size; }

    /**
     * @brief Sets the file size in bytes.
     *
     * @param size The file size to assign.
     */
    public void setSize(long size) { this.size = size; }

    /**
     * @brief Returns the file type.
     *
     * @return The file type or MIME type.
     */
    public String getType() { return type; }

    /**
     * @brief Sets the file type.
     *
     * @param type The file type or MIME type to assign.
     */
    public void setType(String type) { this.type = type; }

    /**
     * @brief Returns the uploader's user ID.
     *
     * @return The user who uploaded the file.
     */
    public String getUploadedBy() { return uploadedBy; }

    /**
     * @brief Sets the uploader's user ID.
     *
     * @param uploadedBy The owner/uploader to assign.
     */
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    /**
     * @brief Returns the upload date.
     *
     * @return The upload timestamp string.
     */
    public String getUploadDate() { return uploadDate; }

    /**
     * @brief Sets the upload date.
     *
     * @param uploadDate The upload timestamp to assign.
     */
    public void setUploadDate(String uploadDate) { this.uploadDate = uploadDate; }

    /**
     * @brief Returns the additional information text.
     *
     * @return Additional descriptive notes.
     */
    public String getAdditionalInfo() { return additionalInfo; }

    /**
     * @brief Sets the additional information text.
     *
     * @param additionalInfo Additional notes or description to assign.
     */
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
}