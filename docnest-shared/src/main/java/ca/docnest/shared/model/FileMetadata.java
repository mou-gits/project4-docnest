package ca.docnest.shared.model;

public class FileMetadata {

    private String fileId;
    private String filename;
    private long size;
    private String type;
    private String uploadedBy;
    private String uploadDate;
    private String additionalInfo;

    public FileMetadata() {}

    // Existing constructor (keep it)
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

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getUploadDate() { return uploadDate; }
    public void setUploadDate(String uploadDate) { this.uploadDate = uploadDate; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
}