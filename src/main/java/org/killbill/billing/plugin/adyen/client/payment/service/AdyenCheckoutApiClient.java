package org.killbill.billing.plugin.adyen.client.payment.service;

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.ApiError;
import com.adyen.model.checkout.PaymentsDetailsRequest;
import com.adyen.model.checkout.PaymentsRequest;
import com.adyen.model.checkout.PaymentsResponse;
import com.adyen.service.Checkout;
import com.adyen.service.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.jooq.tools.StringUtils;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.*;


public class AdyenCheckoutApiClient {
    final Checkout checkoutApi;
    private static final Logger logger = LoggerFactory.getLogger(AdyenCheckoutApiClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();


    public AdyenCheckoutApiClient(final AdyenConfigProperties adyenConfigProperties, final String countryCode) {
        // initialize the REST client here
        Environment environment = Environment.TEST; //default Adyen environmnet
        String envProperty = adyenConfigProperties.getEnvironment();
        if(!StringUtils.isEmpty(envProperty) && envProperty.equals("LIVE")) {
            environment = Environment.LIVE;
        }

        final Client client = new Client(adyenConfigProperties.getApiKey(countryCode), environment);
        checkoutApi = new Checkout(client);
    }

    @VisibleForTesting
    AdyenCheckoutApiClient(final Checkout checkoutApi) {
        this.checkoutApi = checkoutApi;
    }

    public AdyenCallResult<PaymentsResponse> createPayment(PaymentsRequest request) {
        final AdyenCallResult<PaymentsResponse> result;
        result = callApi(request, new ApiRequest<PaymentsRequest, PaymentsResponse>() {
            @Override
            public PaymentsResponse call() throws ApiException, IOException {
                final PaymentsResponse response = checkoutApi.payments(request);
                logResponse(response);
                return response;
            }
        });

        return result;
    }

    private void logResponse(final PaymentsResponse response) {
        //mask sensitive data from response
        final String logResponse = jsonObject(response);
        logger.info("Checkout API response: \n\n" + logResponse);
    }

    public AdyenCallResult<PaymentsResponse> paymentDetails(PaymentsDetailsRequest request) {
        final AdyenCallResult<PaymentsResponse> result;
        result = callApi(request, new ApiRequest<PaymentsDetailsRequest, PaymentsResponse>() {
            @Override
            public PaymentsResponse call() throws ApiException, IOException {
                final PaymentsResponse response = checkoutApi.paymentsDetails(request);
                logResponse(response);
                return response;
            }
        });

        return result;
    }

    private <REQ, RES> AdyenCallResult<RES> callApi(REQ request, ApiRequest<REQ, RES> apiRequest) {
        final String logRequest = jsonObject(request);
        logger.info("Checkout API request: \n\n" + logRequest);

        final long startTime = System.currentTimeMillis();
        try {
            final RES result = apiRequest.call();
            final long duration = System.currentTimeMillis() - startTime;
            logger.info("Checkout call duration: "+ duration);
            return new SuccessfulAdyenCall<RES>(result, duration);
        } catch (ApiException ex) {
            final long duration = System.currentTimeMillis() - startTime;
            logger.warn("Exception during Adyen request", ex);
            return handleException(ex, duration);
        } catch (IOException ex) {
            final long duration = System.currentTimeMillis() - startTime;
            logger.warn("Exception during Adyen request", ex);
            return handleException(ex, duration);
        }
    }

    private <T> String jsonObject(T logObject) {
        String resultStr;
        try {
            resultStr = mapper.writeValueAsString(logObject);
        } catch (IOException e) {
            logger.info("Unable to convert log object to JSON");
            resultStr = logObject.toString();
        }

        return resultStr;
    }

    private <T> UnSuccessfulAdyenCall<T> handleException(final Exception ex, final long duration) {
        final Throwable rootCause = Throwables.getRootCause(ex);

        logger.info("Checkout API duration="+ duration +" response=exception");
        logger.error("Error sending request:", ex.getMessage());
        if(ex instanceof ApiException) {
            ApiException apiException = (ApiException) ex;
            ApiError apiError = apiException.getError();
            String errorDetails = new StringBuilder()
                    .append("status :" + apiError.getStatus())
                    .append("errorCode :" + apiError.getErrorCode())
                    .append("message: " + apiError.getMessage())
                    .append("type: " +  apiError.getErrorType())
                    .append("pspReference: " + apiError.getPspReference())
                    .toString();
            logger.error("API exception:", errorDetails);
            return new FailedCheckoutApiCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, ex);
        } else if(ex instanceof IOException) {
            return new FailedCheckoutApiCall<T>(REQUEST_NOT_SEND, rootCause, ex);
        } else {
            return new FailedCheckoutApiCall<T>(UNKNOWN_FAILURE, rootCause, ex);
        }
    }

    private interface ApiRequest<REQ, RES> {
        RES call() throws ApiException, IOException;
    }
}
