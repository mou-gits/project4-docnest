package ca.docnest.model;

public class FileMetadata {
    private String filename;
    private long size;
    private String type;
    private String uploadedBy;
    private String uploadDate;
    private String info;

    public FileMetadata() {}

    public FileMetadata(String filename, long size, String type,
                        String uploadedBy, String uploadDate, String info) {
        this.filename = filename;
        this.size = size;
        this.type = type;
        this.uploadedBy = uploadedBy;
        this.uploadDate = uploadDate;
        this.info = info;
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

    public String getInfo() {
        return info;
    }
}
