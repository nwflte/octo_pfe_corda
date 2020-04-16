package com.octo.dto;

public class ObligationUpdateDTO {
    private String externalId;

    public ObligationUpdateDTO() {
    }

    public ObligationUpdateDTO(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
