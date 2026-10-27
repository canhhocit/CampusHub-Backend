package com.campushub.modules.wallet.dto;

public class AdminPayoutActionRequest {
    private String referenceCode;
    private String note;

    public AdminPayoutActionRequest() {
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
