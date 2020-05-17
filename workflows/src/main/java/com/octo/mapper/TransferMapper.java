package com.octo.mapper;

import com.octo.dto.BankTransferDTO;
import com.octo.enums.VirementStatus;
import com.octo.states.InterBankTransferState;

import java.math.BigDecimal;
import java.util.Date;

public class TransferMapper {
    public static BankTransferDTO mapInterBankState(InterBankTransferState state){
        BankTransferDTO bankTransferDTO = new BankTransferDTO();
        bankTransferDTO.setAmount(BigDecimal.valueOf(state.getAmount().getQuantity()/100));
        bankTransferDTO.setExecutionDate(state.getExecutionDate());
        bankTransferDTO.setReceiverRIB(state.getReceiverRIB());
        bankTransferDTO.setSenderRIB(state.getSenderRIB());
        bankTransferDTO.setReference(state.getExternalId());
        bankTransferDTO.setStatus(VirementStatus.EXTERNE_APPROVED.toString());
        bankTransferDTO.setStatusUpdate(new Date());
        return bankTransferDTO;
    }
}
