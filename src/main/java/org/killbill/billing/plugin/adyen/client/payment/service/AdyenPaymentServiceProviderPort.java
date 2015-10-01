/*
 * Copyright 2014-2015 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.killbill.billing.plugin.adyen.client.payment.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.adyen.payment.ModificationResult;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.*;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdyenPaymentServiceProviderPort implements Closeable {

    private final static Logger logger = LoggerFactory.getLogger("adyen.port");

    private final PaymentInfoConverterManagement paymentInfoConverterManagement;
    private final AdyenRequestFactory adyenRequestFactory;
    private final AdyenPaymentRequestSender adyenPaymentRequestSender;

    public AdyenPaymentServiceProviderPort(final PaymentInfoConverterManagement paymentInfoConverterManagement,
                                           final AdyenRequestFactory adyenRequestFactory,
                                           final AdyenPaymentRequestSender adyenPaymentRequestSender) {
        this.paymentInfoConverterManagement = paymentInfoConverterManagement;
        this.adyenRequestFactory = adyenRequestFactory;
        this.adyenPaymentRequestSender = adyenPaymentRequestSender;
    }

    @Override
    public void close() throws IOException {
        adyenPaymentRequestSender.close();
    }

    public PurchaseResult authorise(final Long amount,
                                    final PaymentData paymentData,
                                    final OrderData orderData,
                                    final UserData userData,
                                    final String termUrl,
                                    final SplitSettlementData splitSettlementData) {
        logger.info("authorise Start");

        Preconditions.checkNotNull(paymentData.getPaymentTxnInternalRef(), "paymentTxnInternalRef");

        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();
        Preconditions.checkNotNull(paymentInfo, "paymentInfo");
        Preconditions.checkNotNull(paymentInfo.getPaymentProvider(), "paymentProvider");
        Preconditions.checkNotNull(paymentInfo.getPaymentProvider().getCurrency(), "currency");
        Preconditions.checkNotNull(paymentInfo.getPaymentProvider().getCountryIsoCode(), "countryIsoCode");

        final PaymentRequest request = adyenRequestFactory.createPaymentRequest(amount, orderData, paymentData, userData, termUrl, splitSettlementData);
        return authorise(request, paymentInfo.getPaymentProvider().getCountryIsoCode(), paymentData, !paymentInfo.getPaymentProvider().send3DSTermUrl() ? termUrl : null);
    }

    @VisibleForTesting
    PurchaseResult authorise(final PaymentRequest request, final String countryIsoCode, final PaymentData paymentData, final String termUrl) {
        AdyenCallResult<PaymentResult> adyenCallResult = adyenPaymentRequestSender.authorise(countryIsoCode, request);

        if (!adyenCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtPurchase("authorize", paymentData, adyenCallResult);
        }

        logger.info("received adyen authorize response: {}", adyenCallResult);
        PaymentResult result = adyenCallResult.getResult().get();
        final PurchaseResult purchaseResult;
        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(result.getResultCode());
        if (paymentServiceProviderResult != PaymentServiceProviderResult.REDIRECT_SHOPPER) {
            purchaseResult = new PurchaseResult(paymentServiceProviderResult,
                    result.getAuthCode(),
                    result.getPspReference(),
                    result.getRefusalReason(),
                    result.getResultCode(),
                    paymentData.getPaymentInternalRef());
        } else {
            logger.info("proceed with 3dSecure flow");
            final Map<String, String> formParams = new HashMap<String, String>();
            formParams.put(AdyenPaymentPluginApi.PROPERTY_PA_REQ, result.getPaRequest());
            formParams.put(AdyenPaymentPluginApi.PROPERTY_MD, result.getMd());
            formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_VALUE, result.getDccAmount() == null ? null : String.valueOf(result.getDccAmount().getValue()));
            formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, result.getDccAmount() == null ? null : result.getDccAmount().getCurrency());
            formParams.put(AdyenPaymentPluginApi.PROPERTY_DCC_SIGNATURE, result.getDccSignature());
            formParams.put(AdyenPaymentPluginApi.PROPERTY_ISSUER_URL, result.getIssuerUrl());
            formParams.putAll(extractMpiAdditionalData(result));
            if (termUrl != null) {
                formParams.put(AdyenPaymentPluginApi.PROPERTY_TERM_URL, termUrl);
            }

            purchaseResult = new PurchaseResult(paymentServiceProviderResult,
                    result.getAuthCode(),
                    result.getPspReference(),
                    result.getRefusalReason(),
                    result.getResultCode(),
                    paymentData.getPaymentInternalRef(),
                    result.getIssuerUrl(),
                    formParams);
        }

        logger.info("authorise Done", purchaseResult);
        return purchaseResult;
    }

    private PurchaseResult handleTechnicalFailureAtPurchase(String callKey, PaymentData paymentData, AdyenCallResult<PaymentResult> adyenCallResult) {
        logger.info("payment {} call failed for internalReference: {} because of: {}", callKey, paymentData.getPaymentInternalRef(), adyenCallResult);
        return new PurchaseResult(paymentData.getPaymentInternalRef(), adyenCallResult.getResponseStatus().get());
    }

    public PurchaseResult authorize3DSecure(final Long billedAmount,
                                            final PaymentData<Card> paymentData,
                                            final UserData userData,
                                            final Map<String, String[]> requestParameterMap,
                                            final SplitSettlementData splitSettlementData) {
        logger.info("authorize3DSecure Start {} [}", billedAmount, userData);

        final Card paymentInfo = paymentData.getPaymentInfo();
        final BrowserInfo info = (BrowserInfo) paymentInfoConverterManagement.getBrowserInfoFor3DSecureAuth(billedAmount, paymentData.getPaymentInfo());
        final PaymentRequest3D request = adyenRequestFactory.paymentRequest3d(paymentData.getPaymentInternalRef(),
                paymentInfo,
                info,
                requestParameterMap,
                splitSettlementData,
                userData.getIP(),
                userData.getEmail(),
                userData.getCustomerId());
        AdyenCallResult<PaymentResult> adyenCallResult = adyenPaymentRequestSender.authorise3D(paymentInfo.getPaymentProvider().getCountryIsoCode(), request);

        if (!adyenCallResult.receivedWellFormedResponse()) {
            handleTechnicalFailureAtPurchase("3dSecureAuthorise", paymentData, adyenCallResult);
        }

        logger.info("received adyen 3dSecure authorize response: {}", adyenCallResult);
        PaymentResult result = adyenCallResult.getResult().get();
        final PurchaseResult purchaseResult;
        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(result.getResultCode());
        if (paymentServiceProviderResult != PaymentServiceProviderResult.REDIRECT_SHOPPER) {
            purchaseResult = new PurchaseResult(PaymentServiceProviderResult.getPaymentResultForId(result.getResultCode()),
                    result.getAuthCode(),
                    result.getPspReference(),
                    result.getRefusalReason(),
                    result.getResultCode(),
                    paymentData.getPaymentInternalRef());
        } else {
            final Map<String, String> formParams = new HashMap<String, String>();
            formParams.put("PaReq", result.getPaRequest());
            formParams.put("MD", result.getMd());

            purchaseResult = new PurchaseResult(paymentServiceProviderResult,
                    result.getAuthCode(),
                    result.getPspReference(),
                    result.getRefusalReason(),
                    result.getResultCode(),
                    paymentData.getPaymentInternalRef(),
                    result.getIssuerUrl(),
                    formParams);
        }

        logger.info("authorize3DSecure Done {}", purchaseResult);
        return purchaseResult;
    }

    public PaymentModificationResponse refund(final Long externalAmount,
                                              final PaymentProvider paymentProvider,
                                              final String pspReference,
                                              final SplitSettlementData splitSettlementData) {
        logger.info("refund Start for pspReference {}", pspReference);

        final ModificationRequest modificationRequest = adyenRequestFactory.paymentExecutionToAdyenModificationRequest(paymentProvider, externalAmount, pspReference, splitSettlementData);
        AdyenCallResult<ModificationResult> adyenCallResult = adyenPaymentRequestSender.refund(paymentProvider.getCountryIsoCode(), modificationRequest);

        if (!adyenCallResult.receivedWellFormedResponse()) {
            return handleTechnicalErrorAtModificationRequest("refund", pspReference, adyenCallResult);
        }

        ModificationResult result = adyenCallResult.getResult().get();
        logger.debug("refund for pspReference: {}, got new pspReference: {} and response {}", pspReference, result.getPspReference(), result.getResponse());

        final PaymentModificationResponse paymentModificationResponse = new PaymentModificationResponse(result.getResponse(),
                result.getPspReference(),
                entriesToMap(result.getAdditionalData()));
        logger.info("refund End: {}", paymentModificationResponse);
        return paymentModificationResponse;
    }

    private PaymentModificationResponse handleTechnicalErrorAtModificationRequest(String callKey, String pspReference, AdyenCallResult<ModificationResult> adyenCallResult) {
        logger.info("payment {} call failed for pspRef: {} because of: {}", callKey, pspReference, adyenCallResult);
        return new PaymentModificationResponse(pspReference, adyenCallResult.getResponseStatus().get());
    }

    public PaymentModificationResponse cancel(final PaymentProvider paymentProvider,
                                              final String pspReference,
                                              final SplitSettlementData splitSettlementData) {
        logger.info("cancel Start for pspReference {}", pspReference);

        final ModificationRequest modificationRequest = adyenRequestFactory.paymentExecutionToAdyenModificationRequest(paymentProvider, pspReference, splitSettlementData);
        AdyenCallResult<ModificationResult> adyenCallResult = adyenPaymentRequestSender.cancel(paymentProvider.getCountryIsoCode(), modificationRequest);

        if (!adyenCallResult.receivedWellFormedResponse()) {
            return handleTechnicalErrorAtModificationRequest("refund", pspReference, adyenCallResult);
        }

        ModificationResult result = adyenCallResult.getResult().get();
        logger.debug("cancel for pspReference: {}, got new pspReference: {} and response {}", pspReference, result.getPspReference(), result.getResponse());

        final PaymentModificationResponse response = new PaymentModificationResponse(result.getResponse(),
                result.getPspReference(),
                entriesToMap(result.getAdditionalData()));
        logger.info("cancel End", response);
        return response;
    }

    public PaymentModificationResponse capture(final Long amount,
                                               final PaymentProvider paymentProvider,
                                               final String pspReference,
                                               final SplitSettlementData splitSettlementData) {
        logger.info("capture Start for pspReference {}", pspReference);

        final ModificationRequest modificationRequest = adyenRequestFactory.paymentExecutionToAdyenModificationRequest(paymentProvider, amount, pspReference, splitSettlementData);
        AdyenCallResult<ModificationResult> adyenCallResult = adyenPaymentRequestSender.capture(paymentProvider.getCountryIsoCode(), modificationRequest);

        if (!adyenCallResult.receivedWellFormedResponse()) {
            return handleTechnicalErrorAtModificationRequest("capture", pspReference, adyenCallResult);
        }

        ModificationResult result = adyenCallResult.getResult().get();
        logger.debug("capture for pspReference: {}, got new pspReference: {} and response {}", pspReference, result.getPspReference(), result.getResponse());

        final PaymentModificationResponse response = new PaymentModificationResponse(result.getResponse(),
                result.getPspReference(),
                entriesToMap(result.getAdditionalData()));
        logger.info("capture End", response);
        return response;
    }


    /**
     * Extract MPI specific form data from the result's additional data.
     *
     * @param result result
     * @return form parameters to be sent to the issuerUrl
     */
    private Map<String, String> extractMpiAdditionalData(final PaymentResult result) {
        final AnyType2AnyTypeMap additionalData = result.getAdditionalData();
        final Map<String, String> additionalDataMap = anyType2AnyTypeMapToStringMap(additionalData);
        final String mpiImplementation = additionalDataMap.get(AdyenRequestFactory.MPI_IMPLEMENTATION_TYPE);

        final Map<String, String> mpiData = new HashMap<String, String>();
        if (mpiImplementation != null) {
            final String prefix = mpiImplementation + ".";
            for (final Map.Entry<String, String> e : additionalDataMap.entrySet()) {
                if (e.getKey().startsWith(prefix)) {
                    mpiData.put(e.getKey().substring(prefix.length()), e.getValue());
                }
            }
        }
        mpiData.put(AdyenRequestFactory.MPI_IMPLEMENTATION_TYPE, mpiImplementation);
        return mpiData;
    }

    /**
     * Convert {@link AnyType2AnyTypeMap} to {@link Map}.
     *
     * @param additionalData additionalData
     * @return map
     */
    private Map<String, String> anyType2AnyTypeMapToStringMap(final AnyType2AnyTypeMap additionalData) {
        final Map<String, String> additionalDataMap = new HashMap<String, String>();
        if (additionalData != null) {
            for (final AnyType2AnyTypeMap.Entry e : additionalData.getEntry()) {
                additionalDataMap.put(e.getKey().toString(), e.getValue().toString());
            }
        }
        return additionalDataMap;
    }

    private Map<Object, Object> entriesToMap(final AnyType2AnyTypeMap dataMap) {
        Map<Object, Object> map = null;
        if (dataMap != null) {
            final List<AnyType2AnyTypeMap.Entry> result = dataMap.getEntry();
            map = new HashMap<Object, Object>(result.size());
            for (final AnyType2AnyTypeMap.Entry entry : result) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }
}
