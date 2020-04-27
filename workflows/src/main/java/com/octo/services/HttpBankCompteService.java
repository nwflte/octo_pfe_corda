package com.octo.services;

import com.octo.states.InterBankTransferState;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Currency;

@CordaService
public class HttpBankCompteService extends SingletonSerializeAsToken {
    private final Logger logger = LoggerFactory.getLogger(HttpBankCompteService.class);
    private final ServiceHub serviceHub;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final String bankABaseURL = "http://localhost:8081";
    private static final String bankBBaseURL = "http://localhost:8082";
    private static final String centralBankBaseURL = "http://localhost:8083";

    public HttpBankCompteService(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    public boolean verifyAccountExists(String rib, Party verifyingParty) throws IOException {
        String baseURL = getBaseURL(verifyingParty);
        final String url = baseURL.concat("/api/comptes/exists/").concat(rib);
        try {
            return Boolean.parseBoolean(sendGet(url));
        } finally {
            close();
        }
    }

    public boolean verifyAccountEligibleForTransfer(InterBankTransferState transfer, Party verifyingParty) throws IOException {
        String baseURL = getBaseURL(verifyingParty);
        final String url = baseURL.concat("/api/comptes/eligible?rib=").concat(transfer.getReceiverRIB())
                .concat("&amount="+transfer.getAmount().getQuantity()*100);
        final String url2 = baseURL.concat("/api/comptes/eligible");
        final String remoteTest = "http://www.mocky.io/v2/5ea5b47e320000fe28ac27e1";
        try {
            return Boolean.parseBoolean(sendGet(url2));
        } finally {
            close();
        }
    }

    private String sendGet(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        logger.info("about to send HTTP GET to ", url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            logger.info("Received response from " + url, response);

            // Get HttpResponse Status
            System.out.println(response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();

            Header headers = entity.getContentType();
            System.out.println(headers);

            // return it as a String
            String result = EntityUtils.toString(entity);
            System.out.println(result);
            return result;
        }

    }

    // TODO use serializer to send any object as JSON
    private String sendPost(String url, Amount<Currency> data) throws IOException {
        HttpPost request = new HttpPost(url);
        String json = "{\"amount\":" + data.getQuantity()*100 + "}";
        request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {

            // Get HttpResponse Status
            System.out.println(response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            Header headers = entity.getContentType();
            System.out.println(headers);

            // return it as a String
            String result = EntityUtils.toString(entity);
            System.out.println(result);
            return result;
        }

    }


    private void close() throws IOException {
        httpClient.close();
    }

    private String getBaseURL(Party bank) {
        String organizationName = bank.getName().getOrganisation();
        switch (organizationName) {
            case "BankA":
                return bankABaseURL;
            case "BankB":
                return bankBBaseURL;
            case "CentralBank":
                return centralBankBaseURL;
            default:
                throw new IllegalArgumentException("Can't find base Url for " + organizationName);
        }
    }
}
