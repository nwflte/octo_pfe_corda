package com.octo.service.impl;

import com.octo.CONSTANTS;
import com.octo.dto.BankTransferDTO;
import com.octo.flows.AtomicExchangeDDR;
import com.octo.mapper.StateMapper;
import com.octo.schemas.PersistentInterBankTransfer;
import com.octo.service.InterBankTransferService;
import com.octo.service.NodeService;
import com.octo.states.InterBankTransferState;
import com.octo.utils.QueryUtils;
import com.octo.utils.Utils;
import com.octo.web.NodeRPCConnection;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.springframework.stereotype.Service;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class InterBankTransferServiceVaultImpl implements InterBankTransferService {

    private final CordaRPCOps proxy;
    private final NodeService nodeService;

    public InterBankTransferServiceVaultImpl(NodeRPCConnection rpc, NodeService nodeService) {
        this.proxy = rpc.proxy;
        this.nodeService = nodeService;
    }

    @Override
    public List<PersistentInterBankTransfer> loadAll() {
        List<StateAndRef<InterBankTransferState>> states = proxy.vaultQuery(InterBankTransferState.class).getStates();
        return StateMapper.map(states, PersistentInterBankTransfer.class);
    }

    @Override
    public Optional<PersistentInterBankTransfer> findById(String id) {
        List<StateAndRef<InterBankTransferState>> list = proxy.vaultQueryByCriteria(QueryUtils.linearStateByExternalId(id),
                InterBankTransferState.class).getStates();
        return !list.isEmpty() ? Optional.of((PersistentInterBankTransfer) StateMapper.map(list.get(0))) : Optional.empty();
    }

    @Override
    public SignedTransaction transfer(BankTransferDTO dto) throws ExecutionException, InterruptedException {
        Amount<Currency> amount = new Amount<>(Utils.toCentimes(dto.getAmount()), CONSTANTS.MAD);
        Party receiverBank = nodeService.getPartyFromRIB(dto.getReceiverRIB());
        return proxy.startTrackedFlowDynamic(AtomicExchangeDDR.Initiator.class, dto.getSenderRIB(), dto.getReceiverRIB(),
                receiverBank, amount, dto.getExecutionDate()).getReturnValue().get();
    }
}
