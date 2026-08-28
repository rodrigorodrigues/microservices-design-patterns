package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;

public class Passkey {
    private String id;
    private String label;
    private String created;

    @SerializedName("lastUsed")
    private String lastUsed;

    @SerializedName("signatureCount")
    private long signatureCount;

    @SerializedName("lastModifiedByUser")
    private String lastModifiedByUser;

    @SerializedName("lastModifiedDate")
    private String lastModifiedDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }
    public String getLastUsed() { return lastUsed; }
    public void setLastUsed(String lastUsed) { this.lastUsed = lastUsed; }
    public long getSignatureCount() { return signatureCount; }
    public void setSignatureCount(long signatureCount) { this.signatureCount = signatureCount; }
    public String getLastModifiedByUser() { return lastModifiedByUser; }
    public void setLastModifiedByUser(String lastModifiedByUser) { this.lastModifiedByUser = lastModifiedByUser; }
    public String getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(String lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
}
