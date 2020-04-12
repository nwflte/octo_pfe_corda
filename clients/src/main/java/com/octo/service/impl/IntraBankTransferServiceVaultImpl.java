package com.octo.service.impl;

import com.octo.Constants;
import com.octo.dto.BankTransferDTO;
import com.octo.flows.RecordIntraBankTransfer;
import com.octo.mapper.StateMapper;
import com.octo.schemas.PersistentIntraBankTransfer;
import com.octo.service.IntraBankTransferService;
import com.octo.states.IntraBankTransferState;
import com.octo.utils.QueryUtils;
import com.octo.utils.Utils;
import com.octo.web.NodeRPCConnection;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.springframework.stereotype.Service;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class IntraBankTransferServiceVaultImpl implements IntraBankTransferService {

    private final CordaRPCOps proxy;

    public IntraBankTransferServiceVaultImpl(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @Override
    public List<PersistentIntraBankTransfer> loadAll() {
        List<StateAndRef<IntraBankTransferState>> states = proxy.vaultQuery(IntraBankTransferState.class).getStates();
        return StateMapper.map(states, PersistentIntraBankTransfer.class);
    }

    @Override
    public Optional<PersistentIntraBankTransfer> findById(String id) {
        List<StateAndRef<IntraBankTransferState>> list = proxy.vaultQueryByCriteria(QueryUtils.linearStateByExternalId(id),
                IntraBankTransferState.class).getStates();
        return !list.isEmpty() ? Optional.of((PersistentIntraBankTransfer) StateMapper.map(list.get(0))) : Optional.empty();
    }

    @Override
    public SignedTransaction transfer(BankTransferDTO dto) throws ExecutionException, InterruptedException {
        Amount<Currency> amount = new Amount<>(Utils.toCentimes(dto.getAmount()), Constants.MAD);
        return proxy.startTrackedFlowDynamic(RecordIntraBankTransfer.Initiator.class, amount, dto.getSenderRIB(), dto.getReceiverRIB(),
                dto.getExecutionDate()).getReturnValue().get();
    }
}
