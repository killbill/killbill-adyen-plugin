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

package org.killbill.billing.plugin.adyen.api.mapping;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.Acquirer;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Recurring;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.clock.Clock;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.*;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_ADDRESS1;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_ADDRESS2;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_CITY;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_STATE;
import static org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi.PROPERTY_ZIP;

public abstract class PaymentInfoMappingService {
    public static PaymentInfo toPaymentInfo(final String merchantAccount,
                                            @Nullable final String countryCode,
                                            final AdyenConfigProperties configuration,
                                            final Clock clock,
                                            @Nullable final AccountData account,
                                            @Nullable final AdyenPaymentMethodsRecord paymentMethodsRecord,
                                            final Iterable<PluginProperty> properties) {
        final PaymentInfo paymentInfo;


        if (paymentMethodsRecord == null) {
            paymentInfo = WebPaymentFrontendMappingService.toPaymentInfo(merchantAccount, configuration, clock, properties);
        } else {
            final String recurringDetailReference = PluginProperties.getValue(PROPERTY_RECURRING_DETAIL_ID, paymentMethodsRecord.getToken(), properties);
            if (recurringDetailReference != null) {
                paymentInfo = RecurringMappingService.toPaymentInfo(paymentMethodsRecord, properties);
            } else {
                final String ddAccountNumber = PluginProperties.findPluginPropertyValue(PROPERTY_DD_ACCOUNT_NUMBER, properties);
                if (ddAccountNumber != null) {
                    paymentInfo = SepaDirectDebitMappingService.toPaymentInfo(account, paymentMethodsRecord, properties);
                } else {
                    // Will be used as a fallback
                    paymentInfo = CardMappingService.toPaymentInfo(paymentMethodsRecord, properties);
                }
            }
        }

        setBrowserInfo(paymentInfo, properties);
        set3DSFields(paymentInfo, properties);
        set3DS2Fields(paymentInfo, properties);
        setBillingAddress(countryCode, account, paymentInfo, paymentMethodsRecord, properties);
        setCaptureDelayHours(paymentInfo, properties);
        setContractAndContinuousAuthentication(paymentInfo, properties);
        setInstallments(paymentInfo, properties);
        setSelectedBrand(paymentInfo, properties);
        setAcquirer(configuration, paymentInfo, properties);

        return paymentInfo;
    }

    private static void setBrowserInfo(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        final String acceptHeader = PluginProperties.findPluginPropertyValue(PROPERTY_ACCEPT_HEADER, properties);
        final String userAgent = PluginProperties.findPluginPropertyValue(PROPERTY_USER_AGENT, properties);
        final String colorDepth = PluginProperties.findPluginPropertyValue(PROPERTY_COLOR_DEPTH, properties);
        final String javaEnabled = PluginProperties.findPluginPropertyValue(PROPERTY_JAVA_ENABLED, properties);
        final String javaScriptEnabled = PluginProperties.findPluginPropertyValue(PROPERTY_JAVA_SCRIPT_ENABLED, properties);
        final String browserLanguage = PluginProperties.findPluginPropertyValue(PROPERTY_BROWSER_LANGUAGE, properties);
        final String screenHeight = PluginProperties.findPluginPropertyValue(PROPERTY_SCREEN_HEIGHT, properties);
        final String screenWidth = PluginProperties.findPluginPropertyValue(PROPERTY_SCREEN_WIDTH, properties);
        final String timeZoneOffset = PluginProperties.findPluginPropertyValue(PROPERTY_BROWSER_TIME_ZONE_OFFSET, properties);
        if (acceptHeader != null) {
            paymentInfo.setAcceptHeader(acceptHeader);
        }
        if (userAgent != null) {
            paymentInfo.setUserAgent(userAgent);
        }
        if (colorDepth != null) {
            paymentInfo.setColorDepth(Integer.valueOf(colorDepth));
        }
        if (javaEnabled != null) {
            paymentInfo.setJavaEnabled(Boolean.valueOf(javaEnabled));
        }
        if (javaScriptEnabled != null) {
            paymentInfo.setJavaScriptEnabled(Boolean.valueOf(javaScriptEnabled));
        }
        if (browserLanguage != null) {
            paymentInfo.setBrowserLanguage(browserLanguage);
        }
        if (screenHeight != null) {
            paymentInfo.setScreenHeight(Integer.valueOf(screenHeight));
        }
        if (screenWidth != null) {
            paymentInfo.setScreenWidth(Integer.valueOf(screenWidth));
        }
        if (timeZoneOffset != null) {
            paymentInfo.setBrowserTimeZoneOffset(Integer.valueOf(timeZoneOffset));
        }
    }

    private static void set3DSFields(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        final String md = PluginProperties.findPluginPropertyValue(PROPERTY_MD, properties);
        if (md != null) {
            paymentInfo.setMd(decode(md));
        }

        final String paRes = PluginProperties.findPluginPropertyValue(PROPERTY_PA_RES, properties);
        if (paRes != null) {
            paymentInfo.setPaRes(decode(paRes));
        }

        final String threeDThreshold = PluginProperties.findPluginPropertyValue(PROPERTY_THREE_D_THRESHOLD, properties);
        if (!Strings.isNullOrEmpty(threeDThreshold)) {
            // Expected in minor units
            paymentInfo.setThreeDThreshold(Long.valueOf(threeDThreshold));
        }

        final String selectedBrand = PluginProperties.findPluginPropertyValue(PROPERTY_SELECTED_BRAND, properties);

        String mpiDataAuthenticationResponse = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_AUTHENTICATION_RESPONSE, properties);
        String mpiDataCavv = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_CAVV, properties);
        String mpiDataCavvAlgorithm = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_CAVV_ALGORITHM, properties);
        String mpiDataDirectoryResponse = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_DIRECTORY_RESPONSE, properties);
        String mpiDataEci = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_ECI, properties);
        String mpiDataXid = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_DATA_XID, properties);
        String mpiImplementationType = PluginProperties.findPluginPropertyValue(PROPERTY_MPI_IMPLEMENTATION_TYPE, properties);

        boolean setMpiData = true;

        if (BRAND_APPLEPAY.equals(selectedBrand) || BRAND_PAYWITHGOOGLE.equals(selectedBrand)) {
            if (mpiDataCavv != null) {
                // these require specific mpi data values
                mpiDataDirectoryResponse = "Y";
                mpiDataAuthenticationResponse = "Y";
                if (mpiDataEci == null || mpiDataEci.isEmpty()) {
                    mpiDataEci = "07";
                }
            }
            // do not generate mpiData at all
            // as that will cause a payment failure with error 'mpi data is not allowed'
            else {
                setMpiData = false;
            }
        }

        if (setMpiData) {
            paymentInfo.setMpiDataAuthenticationResponse(mpiDataAuthenticationResponse);
            paymentInfo.setMpiDataCavv(mpiDataCavv);
            paymentInfo.setMpiDataCavvAlgorithm(mpiDataCavvAlgorithm);
            paymentInfo.setMpiDataDirectoryResponse(mpiDataDirectoryResponse);
            paymentInfo.setMpiDataEci(mpiDataEci);
            paymentInfo.setMpiDataXid(mpiDataXid);
            paymentInfo.setMpiImplementationType(mpiImplementationType);
            if (mpiImplementationType != null) {
                paymentInfo.setMpiImplementationTypeValues(Maps.filterKeys(PluginProperties.toStringMap(properties), Predicates.containsPattern(mpiImplementationType + ".")));
            }
        }

        final String termUrl = PluginProperties.findPluginPropertyValue(PROPERTY_TERM_URL, properties);
        paymentInfo.setTermUrl(termUrl);
    }

    public static void set3DS2Fields(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        final String notificationUrl = PluginProperties.findPluginPropertyValue(PROPERTY_NOTIFICATION_URL, properties);
        final String threeDSServerTransID = PluginProperties.findPluginPropertyValue(PROPERTY_THREEDS_SERVER_TRANS_ID, properties);
        final String threeDS2Token = PluginProperties.findPluginPropertyValue(PROPERTY_THREEDS2_TOKEN, properties);
        final String threeDSMethodURL = PluginProperties.findPluginPropertyValue(PROPERTY_THREEDS_METHOD_URL, properties);
        final String acsTransID = PluginProperties.findPluginPropertyValue(PROPERTY_ACS_TRANS_ID, properties);
        final String messageVersion = PluginProperties.findPluginPropertyValue(PROPERTY_MESSAGE_VERSION, properties);
        final String threeDSCompInd = PluginProperties.findPluginPropertyValue(PROPERTY_THREEDS_COMP_IND, properties);
        final String transStatus = PluginProperties.findPluginPropertyValue(PROPERTY_TRANS_STATUS, properties);
        final String acsChallengeMandated = PluginProperties.findPluginPropertyValue(PROPERTY_ACS_CHALLENGE_MANDATED, properties);
        final String authenticationType = PluginProperties.findPluginPropertyValue(PROPERTY_AUTHENTICATION_TYPE, properties);
        final String dsTransID = PluginProperties.findPluginPropertyValue(PROPERTY_DS_TRANS_ID, properties);
        final String acsReferenceNumber = PluginProperties.findPluginPropertyValue(PROPERTY_ACS_REFERENCE_NUMBER, properties);
        final String acsUrl = PluginProperties.findPluginPropertyValue(PROPERTY_ACS_URL, properties);

        if (notificationUrl != null) {
            paymentInfo.setNotificationUrl(notificationUrl);
        }
        if (threeDSServerTransID != null) {
            paymentInfo.setThreeDSServerTransID(threeDSServerTransID);
        }
        if (threeDS2Token != null) {
            paymentInfo.setThreeDS2Token(threeDS2Token);
        }
        if (threeDSMethodURL != null) {
            paymentInfo.setThreeDSMethodURL(threeDSMethodURL);
        }
        if (acsTransID != null) {
            paymentInfo.setAcsTransID(acsTransID);
        }
        if (messageVersion != null) {
            paymentInfo.setMessageVersion(messageVersion);
        }
        if (threeDSCompInd != null) {
            paymentInfo.setThreeDSCompInd(threeDSCompInd);
        }
        if (transStatus != null) {
            paymentInfo.setTransStatus(transStatus);
        }
        if (acsChallengeMandated != null) {
            paymentInfo.setAcsChallengeMandated(acsChallengeMandated);
        }
        if (authenticationType != null) {
            paymentInfo.setAuthenticationType(authenticationType);
        }
        if (dsTransID != null) {
            paymentInfo.setDsTransID(dsTransID);
        }
        if (acsReferenceNumber != null) {
            paymentInfo.setAcsReferenceNumber(acsReferenceNumber);
        }
        if (acsUrl != null) {
            paymentInfo.setAcsUrl(acsUrl);
        }
    }

    private static void setBillingAddress(@Nullable final String countryCode, @Nullable final AccountData account, final PaymentInfo paymentInfo, @Nullable final AdyenPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties) {
        String street = PluginProperties.getValue(PROPERTY_ADDRESS1, paymentMethodsRecord == null ? null : paymentMethodsRecord.getAddress1(), properties);
        if (street == null && account != null) {
            street = account.getAddress1();
        }
        paymentInfo.setStreet(street);

        String houseNumberOrName = PluginProperties.getValue(PROPERTY_ADDRESS2, paymentMethodsRecord == null ? null : paymentMethodsRecord.getAddress2(), properties);
        if (houseNumberOrName == null && account != null) {
            houseNumberOrName = account.getAddress2();
        }
        paymentInfo.setHouseNumberOrName(houseNumberOrName);

        String city = PluginProperties.getValue(PROPERTY_CITY, paymentMethodsRecord == null ? null : paymentMethodsRecord.getCity(), properties);
        if (city == null && account != null) {
            city = account.getCity();
        }
        paymentInfo.setCity(city);

        String postalCode = PluginProperties.getValue(PROPERTY_ZIP, paymentMethodsRecord == null ? null : paymentMethodsRecord.getZip(), properties);
        if (postalCode == null && account != null) {
            postalCode = account.getPostalCode();
        }
        paymentInfo.setPostalCode(postalCode);

        String stateOrProvince = PluginProperties.getValue(PROPERTY_STATE, paymentMethodsRecord == null ? null : paymentMethodsRecord.getState(), properties);
        if (stateOrProvince == null && account != null) {
            stateOrProvince = account.getStateOrProvince();
        }
        paymentInfo.setStateOrProvince(stateOrProvince);

        paymentInfo.setCountry(countryCode);
    }

    private static void setCaptureDelayHours(final PaymentInfo paymentInfo, final Iterable<PluginProperty> mergedProperties) {
        final String captureDelayHours = PluginProperties.findPluginPropertyValue(PROPERTY_CAPTURE_DELAY_HOURS, mergedProperties);
        if (captureDelayHours != null) {
            paymentInfo.setCaptureDelayHours(Integer.valueOf(captureDelayHours));

        }
    }

    private static void setContractAndContinuousAuthentication(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        String contract = PluginProperties.findPluginPropertyValue(PROPERTY_RECURRING_TYPE, properties);
        if (contract == null && paymentInfo instanceof Recurring) {
            contract = "RECURRING";
        }
        paymentInfo.setContract(contract);

        final String selectedBrand = PluginProperties.findPluginPropertyValue(PROPERTY_SELECTED_BRAND, properties);
        final String contAuthProperty = PluginProperties.findPluginPropertyValue(PROPERTY_CONTINUOUS_AUTHENTICATION, properties);
        final boolean contAuth;
        if (BRAND_APPLEPAY.equals(selectedBrand) || BRAND_PAYWITHGOOGLE.equals(selectedBrand)) {
            // these need to always use ecommerce
            contAuth = false;
            // and won't use recurring
            paymentInfo.setContract(null);
        } else if (contAuthProperty != null) {
            contAuth = Boolean.parseBoolean(contAuthProperty);
        } else {
            // https://docs.adyen.com/developers/recurring-manual
            if ("ONECLICK".equals(contract)) {
                contAuth = false;
            } else if ("RECURRING".equals(contract)) {
                contAuth = true;
            } else {
                // By default, send Ecommerce when we have the CVC, ContAuth otherwise (card on file)
                contAuth = paymentInfo instanceof Card && ((Card) paymentInfo).getCvc() == null;
            }
        }

        if (contAuth) {
            paymentInfo.setShopperInteraction("ContAuth");
        } else {
            paymentInfo.setShopperInteraction("Ecommerce");
        }
    }

    private static void setInstallments(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        final String installments = PluginProperties.findPluginPropertyValue(PROPERTY_INSTALLMENTS, properties);
        if (installments != null) {
            paymentInfo.setInstallments(Integer.valueOf(installments));
        }
    }

    private static void setSelectedBrand(final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        // For the MisterCash payment method, it can be set to maestro (default, to be processed like a Maestro card) or bcmc (to be processed like a MisterCash card)
        // It can also be set to specific values for DineroMail or to force recurring ELV contracts to be handled as SEPA
        final String selectedBrand = PluginProperties.findPluginPropertyValue(PROPERTY_SELECTED_BRAND, properties);
        paymentInfo.setSelectedBrand(selectedBrand);
    }

    private static void setAcquirer(final AdyenConfigProperties configuration, final PaymentInfo paymentInfo, final Iterable<PluginProperty> properties) {
        final String acquirerName = PluginProperties.findPluginPropertyValue(PROPERTY_ACQUIRER, properties);
        String acquirerMid = PluginProperties.findPluginPropertyValue(PROPERTY_ACQUIRER_MID, properties);
        if (acquirerName != null && acquirerMid == null) {
            final Acquirer acquirer = getAcquirers(configuration).get(acquirerName);
            acquirerMid = acquirer.getMid();
        }
        paymentInfo.setAcquirer(acquirerName);
        paymentInfo.setAcquirerMID(acquirerMid);
    }

    private static Map<String, Acquirer> getAcquirers(final AdyenConfigProperties configuration) {
        final Map<String, Acquirer> acquirers = new HashMap<String, Acquirer>();
        final String str = configuration.getAcquirersList();
        // Is there an acquirer list?
        if (!Strings.isNullOrEmpty(str)) {
            // Parse the list of acquirers
            final String[] acquirerList = str.split(",");
            for (final String current : acquirerList) {
                final Acquirer acquirer = parseAcquirer(current);
                if (acquirer != null) {
                    acquirers.put(acquirer.getName(), acquirer);
                }
            }
        }
        return acquirers;
    }

    private static Acquirer parseAcquirer(final String str) {
        final Acquirer acquirer;
        if (Strings.isNullOrEmpty(str)) {
            return null;
        }

        // If the acquirer code is followed by a | and a second value, that value represents the MID.
        final String[] keyValue = str.split("\\|");
        if (keyValue.length > 1) {
            acquirer = new Acquirer(keyValue[0], keyValue[1]);
        } else {
            acquirer = new Acquirer(keyValue[0]);
        }

        return acquirer;
    }
}
