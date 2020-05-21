package com.octo.corda_services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octo.dto.BankTransferDTO;
import com.octo.mapper.TransferMapper;
import com.octo.flows.AtomicExchangeDDR;
import com.octo.flows.RecordIntraBankTransfer;
import com.octo.states.InterBankTransferState;
import com.octo.states.IntraBankTransferState;
import com.rabbitmq.client.*;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.node.services.CordaServiceCriticalFailureException;
import net.corda.core.node.services.ServiceLifecycleEvent;
import net.corda.core.node.services.ServiceLifecycleObserver;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@CordaService
public class VirementProcessingService extends SingletonSerializeAsToken {

    public static final ObjectMapper mapper = new ObjectMapper();
    public static final Logger logger = LoggerFactory.getLogger(VirementProcessingService.class);

    public static final String CORDA_EXCHANGE = "corda_exchange";
    public static final String VIREMENT_QUEUE = "virements";
    public static final String VIREMENT_ROUTING_KEY = "virement";
    public static final String STATUS_QUEUE = "virements_status";
    public static final String STATUS_ROUTING_KEY = "status";
    public static final String RECEIVED_VIR_QUEUE = "virements_received";
    public static final String RECEIVED_ROUTING_KEY = "received";

    private static final String CONTENT_TYPE_JSON = "application/json";
    private Channel channel;
    private final AppServiceHub serviceHub;
    private final Party thisParty;
    private final ConnectionFactory factory = new ConnectionFactory();
    private final Connection conn;

    public VirementProcessingService(AppServiceHub serviceHub) throws IOException, TimeoutException {
        logger.info("Init virement processing service");
        this.serviceHub = serviceHub;
        this.thisParty = serviceHub.getMyInfo().getLegalIdentities().get(0);
        factory.setVirtualHost(thisParty.getName().getOrganisation().toLowerCase());
        conn = factory.newConnection();
        if (thisParty.getName().getOrganisation().equals("BankA"))
            serviceHub.register(AppServiceHub.SERVICE_PRIORITY_LOW, new MyServiceLifeCycleObserver());
    }

    class MyServiceLifeCycleObserver implements ServiceLifecycleObserver {


        @Override
        public void onServiceLifecycleEvent(@NotNull ServiceLifecycleEvent event) throws CordaServiceCriticalFailureException {
            if (event == ServiceLifecycleEvent.STATE_MACHINE_STARTED)
                try {
                    init();
                } catch (IOException e) {
                    throw new CordaServiceCriticalFailureException("Failure while setting RabbitMQ ", e);
                }
        }

        private void init() throws IOException {
            mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            mapper.setDateFormat(new SimpleDateFormat("dd-MM-yyyy hh:mm:ss"));
            logger.info("Created factory");
            channel = conn.createChannel();
            logger.info("Created channel");
            channel.exchangeDeclare(CORDA_EXCHANGE, BuiltinExchangeType.DIRECT, true);

            channel.queueDeclare(VIREMENT_QUEUE, true, false, false, null);
            channel.queueDeclare(STATUS_QUEUE, true, false, false, null);
            channel.queueDeclare(RECEIVED_VIR_QUEUE, true, false, false, null);

            //channel.queueBind(VIREMENT_QUEUE, CORDA_EXCHANGE, VIREMENT_ROUTING_KEY);
            channel.queueBind(STATUS_QUEUE, CORDA_EXCHANGE, STATUS_ROUTING_KEY);
            channel.queueBind(RECEIVED_VIR_QUEUE, CORDA_EXCHANGE, RECEIVED_ROUTING_KEY);

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag,
                                           Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();
                    logger.debug("Received message with deliveryTag={} ", deliveryTag);
                    logger.debug("Received message with consumerTag={} ", consumerTag);
                    logger.debug("Received message with envelope={} ", envelope);
                    // Processing
                    String message = new String(body);
                    logger.debug("Received message as String={} ", message);
                    BankTransferDTO transferDTO = mapper.readerFor(BankTransferDTO.class).readValue(message);
                    logger.debug("Received transfer={} ", transferDTO);

                    try {
                        boolean isInterne = isVirementInterne(transferDTO);
                        SignedTransaction sTx = isInterne ? makeIntraBankTransfer(transferDTO) : makeInterBankTransfer(transferDTO);
                        channel.basicAck(deliveryTag, false);
                        logger.debug("Recording {}banktransfer complete, txId={} ", isInterne ? "intra" : "inter", sTx.getId());
                        // TODO What happens if flows throws exception (Other parties didn't sign, or insufficient funds)?
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            logger.info("Setup Consume messages");
            channel.basicConsume(VIREMENT_QUEUE, false, consumer);

            trackTransferUpdates();
        }

        private void trackTransferUpdates() {
            logger.info("Setting up track state IntraBankTransferState in vault");
            serviceHub.getVaultService().trackBy(IntraBankTransferState.class).getUpdates().subscribe(update -> {
                logger.info("Received intrabank update");
                update.getProduced().forEach(transfer -> {
                    String reference = transfer.getState().getData().getExternalId();
                    try {
                        String message = "{\"reference\":\"" + reference + "\"}";
                        channel.basicPublish(CORDA_EXCHANGE, STATUS_ROUTING_KEY, false,
                                new AMQP.BasicProperties.Builder().deliveryMode(2).contentType(CONTENT_TYPE_JSON).build(),
                                message.getBytes());
                        logger.info("Set up publish transfer reference to RabbitMQ corda queue");
                    } catch (IOException e) {
                        logger.error("Exception while sending intrabank state to queue", e);
                    }
                });
            });

            logger.info("Setting up track state InterBankTransferState in vault");
            serviceHub.getVaultService().trackBy(InterBankTransferState.class).getUpdates().subscribe(update -> {
                logger.info("Received interbank update");
                update.getProduced().forEach(transfer -> {
                    InterBankTransferState transferState = transfer.getState().getData();
                    String reference = transfer.getState().getData().getExternalId();

                    try {
                        if (transferState.getSenderBank().equals(thisParty)) {
                            String message = "{\"reference\":\"" + reference + "\"}";
                            channel.basicPublish(CORDA_EXCHANGE, STATUS_ROUTING_KEY, false,
                                    new AMQP.BasicProperties.Builder().deliveryMode(2).contentType(CONTENT_TYPE_JSON).build(),
                                    message.getBytes());
                            logger.info("Published interbank transfer reference to queue");
                        } else {
                            BankTransferDTO dto = TransferMapper.mapInterBankState(transferState);
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            mapper.writeValue(out, dto);
                            channel.basicPublish(CORDA_EXCHANGE, RECEIVED_ROUTING_KEY, false,
                                    new AMQP.BasicProperties.Builder().deliveryMode(2).contentType(CONTENT_TYPE_JSON).build(),
                                    out.toByteArray());
                            logger.info("Published interbank transfer to queue");
                        }
                    } catch (IOException e) {
                        logger.error("Exception while sending message to queue", e);
                    }
                });
            });
        }

        private SignedTransaction makeIntraBankTransfer(BankTransferDTO transferDTO) throws ExecutionException, InterruptedException {
            // TODO validate argument
            logger.info("Received transfer from queue and about to record it");
            Amount<Currency> amount = new Amount<>(transferDTO.getAmount().longValue() * 100, Currency.getInstance("MAD"));
            RecordIntraBankTransfer.Initiator flow = new RecordIntraBankTransfer.Initiator(amount, transferDTO.getSenderRIB(),
                    transferDTO.getReceiverRIB(), transferDTO.getExecutionDate(), transferDTO.getReference());
            return serviceHub.startFlow(flow).getReturnValue().get();
        }

        private SignedTransaction makeInterBankTransfer(BankTransferDTO dto) throws ExecutionException, InterruptedException {
            // TODO validate argument, choose receiver bank from RIB
            logger.info("Received transfer from queue and about to record it");
            Amount<Currency> amount = new Amount<>(dto.getAmount().longValue() * 100, Currency.getInstance("MAD"));
            AtomicExchangeDDR.Initiator flow = new AtomicExchangeDDR.Initiator(dto.getSenderRIB(), dto.getReceiverRIB(), amount, dto.getExecutionDate(), dto.getReference());
            return serviceHub.startFlow(flow).getReturnValue().get();
        }

        private boolean isVirementInterne(BankTransferDTO dto) {
            String codeBankEmetteur = dto.getSenderRIB().substring(0, 3);
            String codeBankBeneficiaire = dto.getReceiverRIB().substring(0, 3);
            return codeBankBeneficiaire.equals(codeBankEmetteur);
        }
    }
}

