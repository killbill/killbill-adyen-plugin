/*
 * Copyright 2014 Groupon, Inc
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

package org.killbill.billing.plugin.adyen.api;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenHppRequestsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkArgument;
import static org.killbill.billing.plugin.adyen.api.AdyenModelPluginBase.toMap;

public class AdyenPaymentTransactionInfoPlugin extends PluginPaymentTransactionInfoPlugin {

    private static final int ERROR_CODE_MAX_LENGTH = 32;
    private static final Pattern REFUSAL_REASON_PATTERN = Pattern.compile("([0-9]+)\\s*:\\s*(.*)");
    private static final String REFUSAL_REASON_RAW = "refusalReasonRaw";
    private static final Pattern AVS_RESULT_PATTERN = Pattern.compile("([0-9]+)\\s*:\\s*(.*)");

    private final Optional<AdyenResponsesRecord> adyenResponseRecord;

    // This is a map of values from https://docs.adyen.com/developers/risk-management/avs#resultvalues
    // to values from https://en.wikipedia.org/wiki/Address_Verification_System
    private final static Map<String, String> CONVERT_AVS_CODE = new ImmutableMap.Builder()
            .put("-1", "I")  // Not sent => Address not verified
            .put("0", "I")   // Unknown => Address not verified
            .put("1", "A")   // Address matches, but the postal code does not match => Street address matches, but 5-digit and 9-digit postal code do not match
            .put("2", "N")   // Neither postal code nor address match => Street address and ZIP code do not match
            .put("3", "R")   // AVS unavailable => System unavailable
            .put("4", "E")   // AVS not supported for this card type => AVS data is invalid or AVS is not allowed for this card type
            .put("5", "U")   // No AVS data provided => Address information unavailable
            .put("6", "Z")   // Postal code matches, but the address does not match => Street address does not match, but 5-digit postal code matches
            .put("7", "Y")   // Both postal code and address match => Street address and 5-digit postal code match
            .put("8", "I")   // Address not checked, postal code unknown => Address not verified
            .put("9", "B")   // Address matches, postal code unknown => Street address matches, but postal code not verified
            .put("10", "N")  // Address doesn't match, postal code unknown => Street address and ZIP code do not match
            .put("11", "C")  // Postal code not checked, address unknown => Street address and postal code do not match
            .put("12", "B")  // Address matches, postal code not checked => Street address matches, but postal code not verified
            .put("13", "N")  // Address doesn't match, postal code not checked => Street address and ZIP code do not match
            .put("14", "P")  // Postal code matches, address unknown => Postal code matches, but street address not verified
            .put("15", "P")  // Postal code matches, address not checked => Postal code matches, but street address not verified
            .put("16", "Z")  // Postal code doesn't match, address unknown => Street address does not match, but 5-digit postal code matches
            .put("17", "Z")  // Postal code doesn't match, address not checked => Street address does not match, but 5-digit postal code matches
            .put("18", "I")  // Neither postal code nor address were checked => Address not verified
            .put("19", "L")  // Name and postal code matches => Card member's name and billing postal code match, but billing address does not match
            .put("20", "V")  // Name, address and postal code matches => Card member's name, billing address, and billing postal code match
            .put("21", "O")  // Name and address matches => Card member's name and billing address match, but billing postal code does not match
            .put("22", "K")  // Name matches => Card member's name matches but billing address and billing postal code do not match
            .put("23", "F")  // Postal code matches, name doesn't match => Card member's name does not match, but billing postal code matches
            .put("24", "H")  // Both postal code and address matches, name doesn't match => Card member's name does not match. Street address and postal code match
            .put("25", "T")  // Address matches, name doesn't match => Card member's name does not match, but street address matches
            .put("26", "N")  // Neither postal code, address nor name matches => Street address and postal code do not match
            .build();

    private static final Map<String, String> SUPPORTED_ADDITIONAL_DATA_FIELDS = new ImmutableMap.Builder()
            .put("avsResult", "avsResult")
            .put("avsResultRaw", "avsResultCode")
            .put("cvcResult", "cvcResult")
            .put("cvcResultRaw", "cvcResultCode")
            .build();

    private static final String PSP_RESULT_CODE_KEY = "adyenResultCode";


    public AdyenPaymentTransactionInfoPlugin(final UUID kbPaymentId,
                                             final UUID kbTransactionPaymentPaymentId,
                                             final TransactionType transactionType,
                                             final BigDecimal amount,
                                             final Currency currency,
                                             final DateTime utcNow,
                                             final PurchaseResult purchaseResult) {
        super(kbPaymentId,
              kbTransactionPaymentPaymentId,
              transactionType,
              amount,
              currency,
              getPaymentPluginStatus(purchaseResult.getAdyenCallErrorStatus(), purchaseResult.getResult()),
              getGatewayError(purchaseResult),
              truncate(getGatewayErrorCode(purchaseResult)),
              purchaseResult.getPspReference(),
              purchaseResult.getAuthCode(),
              utcNow,
              utcNow,
              extractPluginProperties(purchaseResult));
        adyenResponseRecord = Optional.absent();
    }

    public AdyenPaymentTransactionInfoPlugin(final UUID kbPaymentId,
                                             final UUID kbTransactionPaymentPaymentId,
                                             final TransactionType transactionType,
                                             final BigDecimal amount,
                                             @Nullable final Currency currency,
                                             final Optional<PaymentServiceProviderResult> pspResult,
                                             final DateTime utcNow,
                                             final PaymentModificationResponse paymentModificationResponse) {
        super(kbPaymentId,
              kbTransactionPaymentPaymentId,
              transactionType,
              amount,
              currency,
              getPaymentPluginStatus(paymentModificationResponse.getAdyenCallErrorStatus(), pspResult),
              getGatewayError(paymentModificationResponse),
              truncate(getGatewayErrorCode(paymentModificationResponse)),
              paymentModificationResponse.getPspReference(),
              null,
              utcNow,
              utcNow,
              buildProperties(paymentModificationResponse.getAdditionalData(),
                              pspResult.isPresent() ? pspResult.get().toString() : null));
        adyenResponseRecord = Optional.absent();
    }

    public AdyenPaymentTransactionInfoPlugin(final AdyenResponsesRecord record) {
        this(record, null);
    }

    public AdyenPaymentTransactionInfoPlugin(final AdyenResponsesRecord record, @Nullable final AdyenHppRequestsRecord adyenHppRequestsRecord) {
        super(UUID.fromString(record.getKbPaymentId()),
              UUID.fromString(record.getKbPaymentTransactionId()),
              TransactionType.valueOf(record.getTransactionType()),
              record.getAmount(),
              Strings.isNullOrEmpty(record.getCurrency()) ? null : Currency.valueOf(record.getCurrency()),
              getPaymentPluginStatus(record),
              getGatewayError(record),
              truncate(getGatewayErrorCode(record)),
              record.getPspReference(),
              record.getAuthCode(),
              new DateTime(record.getCreatedDate(), DateTimeZone.UTC),
              new DateTime(record.getCreatedDate(), DateTimeZone.UTC),
              buildProperties(record, adyenHppRequestsRecord));
        adyenResponseRecord = Optional.of(record);
    }

    @Override
    public PaymentPluginStatus getStatus() {
        final PaymentPluginStatus hppTransactionStatus = getStatusFromHpp();
        if (hppTransactionStatus != null) {
            return hppTransactionStatus;
        } else {
            return super.getStatus();
        }
    }

    public PaymentPluginStatus getStatusFromHpp() {
        final String hppTransactionStatus = PluginProperties.findPluginPropertyValue(AdyenPaymentPluginApi.PROPERTY_FROM_HPP_TRANSACTION_STATUS, getProperties());
        if (hppTransactionStatus != null) {
            return PaymentPluginStatus.valueOf(hppTransactionStatus);
        }

        return null;
    }

    public Optional<AdyenResponsesRecord> getAdyenResponseRecord() {
        return adyenResponseRecord;
    }

    private static String getGatewayError(final PurchaseResult purchaseResult) {
        final String refusalResponseMessage = getGatewayError(purchaseResult.getAdditionalData());
        if (refusalResponseMessage != null) {
            return refusalResponseMessage;
        } else if (purchaseResult.getReason() != null) {
            return purchaseResult.getReason();
        } else {
            final Map<String, String> additionalData = purchaseResult.getAdditionalData();
            return getAndFormatExceptionMessage(additionalData.get(PurchaseResult.EXCEPTION_MESSAGE),
                                 getExceptionClass(additionalData));
        }
    }

    private static String getGatewayError(final PaymentModificationResponse paymentModificationResponse) {
        final String refusalResponseMessage = getGatewayError(paymentModificationResponse.getAdditionalData());
        if (refusalResponseMessage != null) {
            return refusalResponseMessage;
        } else {
            final Map<Object, Object> additionalData = paymentModificationResponse.getAdditionalData();
            return getAndFormatExceptionMessage((String)additionalData.get(PurchaseResult.EXCEPTION_MESSAGE),
                                                getExceptionClass(additionalData));
        }
    }

    private static String getGatewayError(final AdyenResponsesRecord record) {
        final Map additionalData = AdyenDao.fromAdditionalData(record.getAdditionalData());
        final String refusalResponseMessage = getGatewayError(additionalData);
        if (refusalResponseMessage != null) {
            return refusalResponseMessage;
        } else if (record.getRefusalReason() != null) {
            return record.getRefusalReason();
        } else {
            return getAndFormatExceptionMessage((String)additionalData.get(PurchaseResult.EXCEPTION_MESSAGE),
                                                getExceptionClass(additionalData));
        }
    }

    private static String getAndFormatExceptionMessage(final String exceptionMessage, final String exceptionClass) {
        if (Strings.isNullOrEmpty(exceptionClass) && Strings.isNullOrEmpty(exceptionClass)) {
            return null;
        }
        if(Strings.isNullOrEmpty(exceptionClass)) {
            return exceptionMessage;
        }
        if(Strings.isNullOrEmpty(exceptionMessage)) {
            return exceptionClass;
        }
        return String.format("%s: %s", exceptionClass, exceptionMessage);
    }

    private static String getGatewayError(@Nullable final Map additionalData) {
        if (additionalData == null) {
            return null;
        }

        final String refusalReasonRaw = (String) additionalData.get(REFUSAL_REASON_RAW);
        if (refusalReasonRaw == null) {
            return null;
        }

        // See https://docs.adyen.com/support/payments-reporting
        final Matcher matcher = REFUSAL_REASON_PATTERN.matcher(refusalReasonRaw);
        if (matcher.matches()) {
            return matcher.group(2);
        } else {
            return null;
        }
    }

    private static String getGatewayErrorCode(final PurchaseResult purchaseResult) {
        return getGatewayErrorCode(purchaseResult.getAdditionalData());
    }

    private static String getGatewayErrorCode(final PaymentModificationResponse paymentModificationResponse) {
        return getGatewayErrorCode(paymentModificationResponse.getAdditionalData());
    }

    private static String getGatewayErrorCode(final AdyenResponsesRecord record) {
        return getGatewayErrorCode(AdyenDao.fromAdditionalData(record.getAdditionalData()));
    }

    private static String getGatewayErrorCode(@Nullable final Map additionalData) {
        if (additionalData == null) {
            return null;
        }

        final String refusalReasonRaw = (String) additionalData.get(REFUSAL_REASON_RAW);
        if (refusalReasonRaw == null) {
            return null;
        }

        // See https://docs.adyen.com/support/payments-reporting
        final Matcher matcher = REFUSAL_REASON_PATTERN.matcher(refusalReasonRaw);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    /**
     * Transforms adyenCallErrorStatus (where there any technical errors?) and pspResult (was the call successful from a business perspective) into the PaymentPluginStatus.
     * Therefor only one of the given params should be present (if there was a technical error we don't have a psp result and the other way around).
     */
    private static PaymentPluginStatus getPaymentPluginStatus(final Optional<AdyenCallErrorStatus> adyenCallErrorStatus, final Optional<PaymentServiceProviderResult> pspResult) {
        checkArgument(adyenCallErrorStatus.isPresent() ^ pspResult.isPresent());
        return (pspResult.isPresent()) ? pspResultToPaymentPluginStatus(pspResult.get()) : adyenCallErrorStatusToPaymentPluginStatus(adyenCallErrorStatus.get());
    }

    private static PaymentPluginStatus getPaymentPluginStatus(final Optional<PaymentServiceProviderResult> pspResult) {
        return (pspResult.isPresent()) ? pspResultToPaymentPluginStatus(pspResult.get()) : adyenCallErrorStatusToPaymentPluginStatus(AdyenCallErrorStatus.UNKNOWN_FAILURE);
    }

    private static PaymentPluginStatus getPaymentPluginStatus(final AdyenResponsesRecord record) {
        if (Strings.isNullOrEmpty(record.getPspResult())) {
            final String adyenCallErrorStatusString = toString(AdyenDao.fromAdditionalData(record.getAdditionalData()).get(PurchaseResult.ADYEN_CALL_ERROR_STATUS));
            final AdyenCallErrorStatus adyenCallErrorStatus;
            if (Strings.isNullOrEmpty(adyenCallErrorStatusString)) {
                adyenCallErrorStatus = AdyenCallErrorStatus.UNKNOWN_FAILURE;
            } else {
                adyenCallErrorStatus = AdyenCallErrorStatus.valueOf(adyenCallErrorStatusString);
            }
            return adyenCallErrorStatusToPaymentPluginStatus(adyenCallErrorStatus);
        } else {
            final PaymentServiceProviderResult paymentResult = PaymentServiceProviderResult.getPaymentResultForId(record.getResultCode() != null ? record.getResultCode() : record.getPspResult());
            final Optional<PaymentServiceProviderResult> pspResult = Optional.of(paymentResult);
            return getPaymentPluginStatus(pspResult);
        }
    }

    private static PaymentPluginStatus adyenCallErrorStatusToPaymentPluginStatus(final AdyenCallErrorStatus adyenCallErrorStatus) {
        switch (adyenCallErrorStatus) {
            case REQUEST_NOT_SEND:
                return PaymentPluginStatus.CANCELED;
            case RESPONSE_ABOUT_INVALID_REQUEST:
                return PaymentPluginStatus.CANCELED;
            case RESPONSE_NOT_RECEIVED:
                return PaymentPluginStatus.UNDEFINED;
            case RESPONSE_INVALID:
                return PaymentPluginStatus.UNDEFINED;
            case UNKNOWN_FAILURE:
                return PaymentPluginStatus.UNDEFINED;
            default:
                return PaymentPluginStatus.UNDEFINED;
        }
    }

    private static PaymentPluginStatus pspResultToPaymentPluginStatus(final PaymentServiceProviderResult pspResult) {
        switch (pspResult) {
            case INITIALISED:
            case REDIRECT_SHOPPER:
            case RECEIVED:
            case PENDING:
            case IDENTIFY_SHOPPER:
            case CHALLENGE_SHOPPER:
                return PaymentPluginStatus.PENDING;
            case AUTHORISED:
                return PaymentPluginStatus.PROCESSED;
            case REFUSED:
                return PaymentPluginStatus.ERROR;
            case CANCELLED:
                return PaymentPluginStatus.CANCELED;
            case ERROR:
            default:
                return PaymentPluginStatus.UNDEFINED;
        }
    }

    private static List<PluginProperty> extractPluginProperties(PurchaseResult purchaseResult) {
        Map<String, String> propertiesMap = new HashMap<>();
        if (purchaseResult.getFormParameter() != null) {
            propertiesMap.putAll(purchaseResult.getFormParameter());
        }

        Map<String, String> additionalData = purchaseResult.getAdditionalData();
        ImmutableList.Builder<PluginProperty> builder = ImmutableList.builder();

        if (additionalData != null) {
            for (Map.Entry<String, String> entry: additionalData.entrySet()) {
                String propName = SUPPORTED_ADDITIONAL_DATA_FIELDS.get(entry.getKey());
                if (propName != null) {
                    if ("avsResultCode".equals(propName)) {
                        //use Unknown (UK) as default
                        propertiesMap.put(propName, CONVERT_AVS_CODE.getOrDefault(entry.getValue(), "UK"));
                    } else if (entry.getValue() != null) {
                        propertiesMap.put(propName, entry.getValue());
                    }
                }
            }

            List<PluginProperty> threeds2Props = AdyenPaymentPluginApi.extractThreeDS2Data(additionalData);

            builder.addAll(threeds2Props);
        }

        builder.addAll(PluginProperties.buildPluginProperties(propertiesMap));
        builder.add(buildPurchaseResultCode(purchaseResult));
        return builder.build();
    }

    private static PluginProperty buildPurchaseResultCode(final PurchaseResult purchaseResult) {
        return new PluginProperty(PSP_RESULT_CODE_KEY, purchaseResult.getResult().orNull(), false);
    }

    private static String toString(final Object obj) {
        return obj == null ? null : obj.toString();
    }

    private static String truncate(@Nullable final String string) {
        if (string == null) {
            return null;
        } else if (string.length() <= ERROR_CODE_MAX_LENGTH) {
            return string;
        } else {
            return string.substring(0, ERROR_CODE_MAX_LENGTH);
        }
    }

    private static String getExceptionClass(final Map additionalData) {
        final String fqClassName = toString(additionalData.get(PurchaseResult.EXCEPTION_CLASS));
        if (fqClassName == null || fqClassName.length() <= ERROR_CODE_MAX_LENGTH) {
            return fqClassName;
        } else {
            // Truncate the class name (thank you Logback! See TargetLengthBasedClassNameAbbreviator)
            return abbreviate(fqClassName, ERROR_CODE_MAX_LENGTH);
        }
    }

    private static String abbreviate(final String fqClassName, final int targetLength) {
        final StringBuilder buf = new StringBuilder(targetLength);
        final int[] dotIndexesArray = new int[16];
        final int[] lengthArray = new int[17];

        final int dotCount = computeDotIndexes(fqClassName, dotIndexesArray);
        if (dotCount == 0) {
            return fqClassName;
        }

        computeLengthArray(fqClassName, dotIndexesArray, lengthArray, dotCount);
        for (int i = 0; i <= dotCount; i++) {
            if (i == 0) {
                buf.append(fqClassName.substring(0, lengthArray[i] - 1));
            } else {
                buf.append(fqClassName.substring(dotIndexesArray[i - 1], dotIndexesArray[i - 1] + lengthArray[i]));
            }
        }

        return buf.toString();
    }

    private static int computeDotIndexes(final String className, final int[] dotArray) {
        int dotCount = 0;
        int k = 0;
        while (true) {
            k = className.indexOf(".", k);
            if (k != -1 && dotCount < dotArray.length) {
                dotArray[dotCount] = k;
                dotCount++;
                k++;
            } else {
                break;
            }
        }
        return dotCount;
    }

    private static void computeLengthArray(final String className, final int[] dotArray, final int[] lengthArray, final int dotCount) {
        int toTrim = className.length() - 32;

        int len;
        for (int i = 0; i < dotCount; i++) {
            int previousDotPosition = -1;
            if (i > 0) {
                previousDotPosition = dotArray[i - 1];
            }
            final int available = dotArray[i] - previousDotPosition - 1;

            if (toTrim > 0) {
                len = (available < 1) ? available : 1;
            } else {
                len = available;
            }
            toTrim -= (available - len);
            lengthArray[i] = len + 1;
        }

        final int lastDotIndex = dotCount - 1;
        lengthArray[dotCount] = className.length() - dotArray[lastDotIndex];
    }

    private static List<PluginProperty> buildProperties(final AdyenResponsesRecord adyenResponsesRecord,
                                                        @Nullable final AdyenHppRequestsRecord adyenHppRequestsRecord) {
        final Map mergedMap = new HashMap();

        if (adyenHppRequestsRecord != null) {
            mergedMap.putAll(toMap(adyenHppRequestsRecord.getAdditionalData()));
        }
        mergedMap.putAll(toMap(adyenResponsesRecord.getAdditionalData()));

        return buildProperties(mergedMap, adyenResponsesRecord != null ? adyenResponsesRecord.getPspResult() : null);
    }

    private static List<PluginProperty> buildProperties(@Nullable final Map data, @Nullable final String pspResult) {
        if (data == null) {
            return ImmutableList.<PluginProperty>of();
        }
        final List<PluginProperty> originalProperties = PluginProperties.buildPluginProperties(data);

        // Add common properties returned by plugins (used by Analytics)
        final Map<String, Object> defaultProperties = new HashMap<String, Object>();
        defaultProperties.put("processorResponse", getGatewayErrorCode(data));
        defaultProperties.put("avsResultCode", data.get("avsResultRaw"));
        defaultProperties.put("cvvResultCode", data.get("cvcResultRaw"));
        defaultProperties.put("payment_processor_account_id", data.get(AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE));
        defaultProperties.put(PSP_RESULT_CODE_KEY, pspResult);
        // Already populated
        //defaultProperties.put("paymentMethod", data.get("paymentMethod"));

        final Iterable<PluginProperty> propertiesWithDefaults = PluginProperties.merge(defaultProperties, originalProperties);
        return ImmutableList.<PluginProperty>copyOf(propertiesWithDefaults);
    }
}
