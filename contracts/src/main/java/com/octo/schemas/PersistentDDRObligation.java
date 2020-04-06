package com.octo.schemas;

import com.octo.enums.DDRObligationStatus;
import com.octo.enums.DDRObligationType;
import net.corda.core.identity.Party;
import net.corda.core.schemas.PersistentState;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "ddr_obligation_states")
public class PersistentDDRObligation extends PersistentState implements Serializable {
    @Column(name = "external_id")
    private String externalId;

    @Column(name = "issuer")
    private Party issuer;

    @Column(name = "requester_date")
    private Date requesterDate;

    @Column(name = "requester_name")
    private Party requester;

    @Column(name = "amount")
    private long amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "owner")
    private Party owner;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private DDRObligationType type;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DDRObligationStatus status;

    @Column(name = "linear_id")
    private UUID linearId;

    public PersistentDDRObligation() {
    }

    public PersistentDDRObligation(String externalId, Party issuer, Date requesterDate, Party requester, long amount, String currency,
                                   Party owner, DDRObligationType type, DDRObligationStatus status, UUID linearId) {
        this.externalId = externalId;
        this.issuer = issuer;
        this.requesterDate = requesterDate;
        this.requester = requester;
        this.amount = amount;
        this.currency = currency;
        this.owner = owner;
        this.type = type;
        this.status = status;
        this.linearId = linearId;
    }

    public String getExternalId() {
        return externalId;
    }

    public Party getIssuer() {
        return issuer;
    }

    public Date getRequesterDate() {
        return requesterDate;
    }

    public Party getRequester() {
        return requester;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Party getOwner() {
        return owner;
    }

    public DDRObligationType getType() {
        return type;
    }

    public DDRObligationStatus getStatus() {
        return status;
    }

    public UUID getLinearId() {
        return linearId;
    }
}