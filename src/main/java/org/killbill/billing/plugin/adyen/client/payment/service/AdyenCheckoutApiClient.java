package org.killbill.billing.plugin.adyen.client.payment.service;

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.checkout.PaymentsDetailsRequest;
import com.adyen.model.checkout.PaymentsRequest;
import com.adyen.model.checkout.PaymentsResponse;
import com.adyen.service.Checkout;
import com.adyen.service.exception.ApiException;
import org.jooq.tools.StringUtils;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;

import java.io.IOException;

public class AdyenCheckoutApiClient {
    final Checkout checkoutApi;

    public AdyenCheckoutApiClient(final AdyenConfigProperties adyenConfigProperties) {
        // initialize the REST client here
        Environment environment = Environment.TEST; //default Adyen environmnet
        String envProperty = adyenConfigProperties.getEnvironment();
        if(!StringUtils.isEmpty(envProperty) && envProperty.equals("LIVE")) {
            environment = Environment.LIVE;
        }

        final Client client = new Client(adyenConfigProperties.getApiKey(), environment);
        checkoutApi = new Checkout(client);
    }

    public PaymentsResponse createPayment(PaymentsRequest request) throws ApiException, IOException {
        PaymentsResponse response = checkoutApi.payments(request);
        return response;
    }

    public PaymentsResponse paymentDetails(PaymentsDetailsRequest request) throws ApiException, IOException {
        PaymentsResponse response = checkoutApi.paymentsDetails(request);
        return response;
    }
}
