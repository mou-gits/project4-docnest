package ca.docnest.shared.protocol;

public class FileRecord {

    public String filename;
    public long size;
    public String owner;
    public String mime;
    public String uploadedAt;
    public String additionalInfo;

    public FileRecord() {}

    public FileRecord(String filename, long size, String owner,
                      String mime, String uploadedAt, String additionalInfo) {
        this.filename = filename;
        this.size = size;
        this.owner = owner;
        this.mime = mime;
        this.uploadedAt = uploadedAt;
        this.additionalInfo = additionalInfo;
    }
}