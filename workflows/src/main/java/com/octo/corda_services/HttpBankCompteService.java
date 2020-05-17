package com.octo.corda_services;

import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

@CordaService
public class HttpBankCompteService extends SingletonSerializeAsToken {
    private final ServiceHub serviceHub;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final String baseURL = "http://localhost:10050/api/comptes/exists/";

    public HttpBankCompteService(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    public String verifyAccountExists(String rib) throws Exception {
        final String url = baseURL.concat(rib);
        try {
            return sendGet(url);
        } finally {
            close();
        }
    }

    private void close() throws IOException {
        httpClient.close();
    }

    private String sendGet(String url) throws Exception {

        HttpGet request = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(request)) {

            // Get HttpResponse Status
            System.out.println(response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            Header headers = entity.getContentType();
            System.out.println(headers);

            if (entity == null) return null;

            // return it as a String
            String result = EntityUtils.toString(entity);
            System.out.println(result);
            return result;
        }

    }
}
