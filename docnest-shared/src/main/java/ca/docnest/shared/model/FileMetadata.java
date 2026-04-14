package ca.docnest.shared.model;

public class FileMetadata {
    private String filename;
    private long size;
    private String type;
    private String uploadedBy;
    private String uploadDate;
    private String additionalInfo;

    public FileMetadata() {}

    public FileMetadata(String filename,
                        long size,
                        String type,
                        String uploadedBy,
                        String uploadDate,
                        String additionalInfo) {
        this.filename = filename;
        this.size = size;
        this.type = type;
        this.uploadedBy = uploadedBy;
        this.uploadDate = uploadDate;
        this.additionalInfo = additionalInfo;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }
}
