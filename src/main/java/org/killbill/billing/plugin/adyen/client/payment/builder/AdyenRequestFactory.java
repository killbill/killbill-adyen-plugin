/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.OrderData;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

public class AdyenRequestFactory {

    public static final String MPI_IMPLEMENTATION_TYPE = "mpiImplementationType";

    private static final Set<String> NOT_ADDITIONAL_DATA = ImmutableSet.<String>of("md",
                                                                                   "merchantAccount",
                                                                                   "shopperIP",
                                                                                   MPI_IMPLEMENTATION_TYPE);

    private final PaymentInfoConverterManagement paymentInfoConverterManagement;
    private final AdyenConfigProperties adyenConfigProperties;
    private final Signer signer;

    public AdyenRequestFactory(final PaymentInfoConverterManagement paymentInfoConverterManagement,
                               final AdyenConfigProperties adyenConfigProperties,
                               final Signer signer) {
        this.paymentInfoConverterManagement = paymentInfoConverterManagement;
        this.adyenConfigProperties = adyenConfigProperties;
        this.signer = signer;
    }

    public PaymentRequest createPaymentRequest(final Long amount,
                                               final OrderData orderData,
                                               final PaymentData paymentData,
                                               final UserData userData,
                                               final String termUrl,
                                               @Nullable final SplitSettlementData splitSettlementData) {
        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();
        return new PaymentRequestBuilder(paymentInfo, paymentInfoConverterManagement, orderData.getHolderName()).withAmount(paymentInfo.getPaymentProvider().getCurrency().getCurrencyCode(), amount)
                                                                                                                .withMerchantAccount(getMerchantAccount(paymentInfo))
                                                                                                                .withRecurringContractForUser()
                                                                                                                .withSelectedRecurringDetailReference()
                                                                                                                .withReference(paymentData.getPaymentTxnInternalRef())
                                                                                                                .withShopperEmail(userData.getEmail())
                                                                                                                .withShopperIp(userData.getIP())
                                                                                                                .withShopperName(userData.getFirstName(), userData.getLastName())
                                                                                                                .withShopperReference(userData.getCustomerId() == null ? null : String.valueOf(userData.getCustomerId()))
                                                                                                                .withBrowserInfo(amount)
                                                                                                                .withReturnUrl(termUrl)
                                                                                                                .withSplitSettlementData(splitSettlementData)
                                                                                                                .build();
    }

    public PaymentRequest3D paymentRequest3d(final String reference,
                                             final Card card,
                                             final BrowserInfo info,
                                             final Map<String, String> requestParameterMap,
                                             @Nullable final SplitSettlementData splitSettlementData,
                                             final String customerIp,
                                             final String customerEmail,
                                             final String customerId) {
        return new PaymentRequest3DBuilder().withMerchantAccount(getMerchantAccount(card))
                                            .withBrowserInfo(info)
                                            .withMd(card.getMd())
                                            .withPaResponse(card.getPaRes())
                                            .withShopperIP(customerIp)
                                            .withShopperEmail(customerEmail)
                                            .withShopperReference(customerId)
                                            .withReference(reference)
                                            .withRecurring(card.getPaymentProvider())
                                            .withSplitSettlementData(splitSettlementData)
                                            .addAdditionalData(createMpiAdditionalData(requestParameterMap))
                                            .build();
    }

    public ModificationRequest paymentExecutionToAdyenModificationRequest(final PaymentProvider paymentProvider,
                                                                          final String pspReference,
                                                                          @Nullable final SplitSettlementData splitSettlementData) {
        return new ModificationRequestBuilder().withOriginalReference(pspReference)
                                               .withMerchantAccount(getMerchantAccount(paymentProvider))
                                               .withSplitSettlementData(splitSettlementData)
                                               .build();
    }

    public ModificationRequest paymentExecutionToAdyenModificationRequest(final PaymentProvider paymentProvider,
                                                                          final Long amount,
                                                                          final String pspReference,
                                                                          @Nullable final SplitSettlementData splitSettlementData) {
        return new ModificationRequestBuilder().withAmount(paymentProvider.getCurrency().getCurrencyCode(), amount)
                                               .withOriginalReference(pspReference)
                                               .withMerchantAccount(getMerchantAccount(paymentProvider))
                                               .withSplitSettlementData(splitSettlementData)
                                               .build();
    }

    public Map<String, String> createHppRequest(final Long amount,
                                                final PaymentData paymentData,
                                                final OrderData orderData,
                                                final UserData userData,
                                                final String serverUrl,
                                                final String resultUrl,
                                                @Nullable final SplitSettlementData splitSettlementData) throws SignatureGenerationException {
        final PaymentProvider paymentProvider = paymentData.getPaymentInfo().getPaymentProvider();
        final String countryIsoCode = paymentProvider.getCountryIsoCode();
        final String currencyIsoCode = paymentProvider.getCurrency().getCurrencyCode();
        final DateTime shipBeforeDate = orderData.getShipBeforeDate();
        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();
        final String customerEmail = userData.getEmail();
        final String customerId = userData.getCustomerId();
        final Locale customerLocale = userData.getCustomerLocale();

        return getHppParamsMap(amount,
                               paymentProvider,
                               serverUrl,
                               resultUrl,
                               countryIsoCode,
                               currencyIsoCode,
                               paymentData.getPaymentInternalRef(),
                               shipBeforeDate,
                               paymentInfo,
                               splitSettlementData,
                               customerEmail,
                               customerId,
                               customerLocale);
    }

    @VisibleForTesting
    Map<String, String> getHppParamsMap(final Long amount,
                                        final PaymentProvider paymentProvider,
                                        final String serverUrl,
                                        final String resultUrl,
                                        final String countryIsoCode,
                                        final String currencyIsoCode,
                                        final String reference,
                                        final DateTime shipBeforeDate,
                                        final PaymentInfo paymentInfo,
                                        @Nullable final SplitSettlementData splitSettlementData,
                                        final String customerEmail,
                                        final String customerId,
                                        final Locale customerLocale) throws SignatureGenerationException {
        final String countryCode = AdyenConfigProperties.gbToUK(customerLocale.getCountry());

        final String skin = adyenConfigProperties.getSkin(countryIsoCode);
        Preconditions.checkState(!Strings.isNullOrEmpty(skin), "skin for " + countryIsoCode);

        final String shipBeforeDateString = new DateTime(shipBeforeDate).toString(adyenConfigProperties.getShipBeforeDatePattern());
        final String sessionValidityString = getSessionValidity(15).toString(adyenConfigProperties.getSessionValidityDatePattern());

        final String merchantAccount = getMerchantAccount(countryIsoCode);

        final HPPRequestBuilder builder = new HPPRequestBuilder().withCountryCode(countryCode)
                                                                 .withMerchantReference(reference)
                                                                 .withPaymentAmount(amount)
                                                                 .withCurrencyCode(currencyIsoCode)
                                                                 .withShipBeforeDate(shipBeforeDateString)
                                                                 .withShopperEmail(customerEmail)
                                                                 .withShopperReference(customerId)
                                                                 .withRecurringContract(paymentProvider)
                                                                 .withResURL(Strings.nullToEmpty(serverUrl) + Strings.nullToEmpty(resultUrl))
                                                                 .withSessionValidity(sessionValidityString)
                                                                 .withBrandCodeAndOrAllowedMethods(paymentInfo)
                                                                 .withShopperLocale(paymentProvider.getPaymentType(), customerLocale)
                                                                 .withSkinCode(skin)
                                                                 .withMerchantAccount(merchantAccount);

        final String merchantSignature = signer.computeSignature(amount,
                                                                 currencyIsoCode,
                                                                 shipBeforeDateString,
                                                                 reference,
                                                                 skin,
                                                                 merchantAccount,
                                                                 customerEmail,
                                                                 customerId,
                                                                 paymentInfo,
                                                                 sessionValidityString);
        builder.withMerchantSig(merchantSignature);

        if (splitSettlementData != null) {
            final Map<String, String> splitSettlementParameters = new SplitSettlementParamsBuilder().createSignedParamsFrom(splitSettlementData,
                                                                                                                            merchantSignature,
                                                                                                                            signer,
                                                                                                                            adyenConfigProperties.getHmacSecret(countryIsoCode));
            builder.withSplitSettlementParameters(splitSettlementParameters);
        }

        return builder.build();
    }

    private String getMerchantAccount(final PaymentInfo paymentInfo) {
        final PaymentProvider paymentProvider = paymentInfo.getPaymentProvider();
        return getMerchantAccount(paymentProvider);
    }

    private String getMerchantAccount(final PaymentProvider paymentProvider) {
        final String countryIsoCode = paymentProvider.getCountryIsoCode();
        return getMerchantAccount(countryIsoCode);
    }

    private String getMerchantAccount(final String countryIsoCode) {
        final String merchantAccount = adyenConfigProperties.getMerchantAccount(countryIsoCode);
        Preconditions.checkState(!Strings.isNullOrEmpty(merchantAccount), "merchantAccount for country " + countryIsoCode);
        return merchantAccount;
    }

    private DateTime getSessionValidity(final int minutes) {
        return new DateTime(DateTimeZone.UTC).plusMinutes(minutes);
    }

    /**
     * Create or re-use and existing additional data {@link AnyType2AnyTypeMap}
     * and fill it with mpi specific parameters handed to us by Adyen.
     *
     * @param requestParameterMap parameters send in the request from Adyen
     * @return additional data to be incorporated in the request
     */
    List<AnyType2AnyTypeMap.Entry> createMpiAdditionalData(final Map<String, String> requestParameterMap) {
        final List<AnyType2AnyTypeMap.Entry> entries = new ArrayList<AnyType2AnyTypeMap.Entry>();
        final String mpiImplementationType = requestParameterMap.get(MPI_IMPLEMENTATION_TYPE);
        if (mpiImplementationType != null) {
            addAdditionalDataEntry(entries, MPI_IMPLEMENTATION_TYPE, mpiImplementationType);
            for (final Map.Entry<String, String> e : requestParameterMap.entrySet()) {
                addRequestParameterEntry(e.getKey(), new String[]{e.getValue()}, mpiImplementationType + ".", entries);
            }
        }
        return entries;
    }

    private void addRequestParameterEntry(final String parameterKey,
                                          final String[] parameterValues,
                                          final String mpiImplementationTypePrefix,
                                          final List<AnyType2AnyTypeMap.Entry> entries) {
        if (!NOT_ADDITIONAL_DATA.contains(parameterKey)) {
            for (final String value : parameterValues) {
                final String prefixedKey = mpiImplementationTypePrefix + parameterKey;
                addAdditionalDataEntry(entries, prefixedKey, value);
            }
        }
    }

    /**
     * Add a new key/value pair to the {@link AnyType2AnyTypeMap.Entry} list.
     *
     * @param entries list of entries
     * @param key     key
     * @param value   value
     */
    private void addAdditionalDataEntry(final List<AnyType2AnyTypeMap.Entry> entries, final String key,
                                        final String value) {
        final AnyType2AnyTypeMap.Entry entry = new AnyType2AnyTypeMap.Entry();
        entry.setKey(key);
        entry.setValue(value);
        entries.add(entry);
    }
}
