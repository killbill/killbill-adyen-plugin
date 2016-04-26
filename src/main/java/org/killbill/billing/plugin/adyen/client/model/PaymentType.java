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

package org.killbill.billing.plugin.adyen.client.model;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import static org.killbill.billing.plugin.adyen.client.model.PaymentType.CreditCardSchemaType.IS_CREDITCARD;
import static org.killbill.billing.plugin.adyen.client.model.PaymentType.CreditCardSchemaType.NO_CREDITCARD;
import static org.killbill.billing.plugin.adyen.client.model.WebFrontendType.FORWARDING_FRONTEND;
import static org.killbill.billing.plugin.adyen.client.model.WebFrontendType.POPUP_FRONTEND;

public enum PaymentType {

    ELV(1, "elv", null, NO_CREDITCARD),
    // "card" is a magic word, see section 2.9 of the HPP Integration manual
    CREDITCARD(2, new String[]{"card", "visa", "mc", "bijcard", "visadankort"}, null, IS_CREDITCARD), // gc & allpago don't support multiple cards through one type
    MAESTROUK(3, new String[]{"maestrouk", "solo"}, null, NO_CREDITCARD),
    AMEX(4, "amex", null, IS_CREDITCARD),
    CARTE_BLEUE(5, "carte_bleue", null, IS_CREDITCARD),
    DIRECT_EBANKING(6, "directEbanking", FORWARDING_FRONTEND, NO_CREDITCARD),
    PAYPAL(7, "paypal", FORWARDING_FRONTEND, NO_CREDITCARD),
    IDEAL(8, "ideal", FORWARDING_FRONTEND, NO_CREDITCARD),
    DOTPAY(9, "dotpay", FORWARDING_FRONTEND, NO_CREDITCARD),
    LASER(10, "laser", null, IS_CREDITCARD),
    BOLETO_BANCARIO(11, "boleto", FORWARDING_FRONTEND, NO_CREDITCARD), // we use wpf so we don't need a brandcode for allpago boleto
    MAESTRO(12, "maestro", null, NO_CREDITCARD),
    VISA(13, "visa", null, IS_CREDITCARD),
    MASTERCARD(14, "mc", null, IS_CREDITCARD),
    DINERO_MAIL(15, "dineromail", FORWARDING_FRONTEND, NO_CREDITCARD),
    VERKKOMAKSUT(16, "ebanking_FI", FORWARDING_FRONTEND, NO_CREDITCARD),
    HIPERCARD(17, "hipercard", null, IS_CREDITCARD),
    AURA(18, "aura", null, IS_CREDITCARD),
    DINERSCLUB(19, "diners", null, IS_CREDITCARD),
    CREDENCIAL(20, "credencial", null, IS_CREDITCARD),
    JCB(21, "jcb", null, IS_CREDITCARD),
    IXE(22, "ixe", null, IS_CREDITCARD),
    HSBC(23, "hsbc", null, IS_CREDITCARD),
    SANTANDER_CREDIT(24, "santandercredit", null, IS_CREDITCARD),
    SANTANDER_DEBIT(25, "santanderdebit", null, IS_CREDITCARD),
    BANCOMER(26, "bancomer", null, IS_CREDITCARD),
    BANORTE(27, "banorte", null, IS_CREDITCARD),
    BANAMEX(28, "banamex", null, IS_CREDITCARD),
    SCOTIABANK(29, "scotiabank", null, IS_CREDITCARD),
    UNIONPAY(30, "unionpay", FORWARDING_FRONTEND, IS_CREDITCARD),
    ALIPAY(31, "alipay", POPUP_FRONTEND, NO_CREDITCARD),
    FUNDS_TRANSFER(32, new String[]{"oft", "bankTransfer_DE"}, FORWARDING_FRONTEND, NO_CREDITCARD),
    TENPAY(33, "tenpay", POPUP_FRONTEND, NO_CREDITCARD),
    BANCNET(34, "bancnet", FORWARDING_FRONTEND, NO_CREDITCARD),
    GLOBEGCASH(35, "globegcash", FORWARDING_FRONTEND, NO_CREDITCARD),
    ISRACARD(36, "isracard", null, IS_CREDITCARD),
    ASIAPAY(37, "asiapay", FORWARDING_FRONTEND, NO_CREDITCARD),
    VISA_HIPOTECARIO(38, "visahipotecario", null, IS_CREDITCARD),
    NARANJA(39, "naranja", null, IS_CREDITCARD),
    CABAL(40, "cabal", null, IS_CREDITCARD),
    SHOPPING(41, "shopping", null, IS_CREDITCARD),
    ITALCRED(42, "italcred", null, IS_CREDITCARD),
    ARGEN(43, "argencard", null, IS_CREDITCARD),
    PSE(44, "pse", FORWARDING_FRONTEND, NO_CREDITCARD),
    SAFETYPAY(45, "safetypay", FORWARDING_FRONTEND, NO_CREDITCARD),
    MISTER_CASH(46, "bcmc", null, NO_CREDITCARD),
    IPAY88(47, "ipay88", FORWARDING_FRONTEND, NO_CREDITCARD),
    CCAVENUE(48, "ccavenue", FORWARDING_FRONTEND, NO_CREDITCARD),
    HDFC(49, "hdfc", FORWARDING_FRONTEND, NO_CREDITCARD),
    PPS(50, "pps", FORWARDING_FRONTEND, NO_CREDITCARD),
    CASH_ON_DELIVERY(51, "cod", null, NO_CREDITCARD),
    BANCODOBRASIL(52, "bancodobrasil", FORWARDING_FRONTEND, NO_CREDITCARD),
    MERCADO_PAGO(53, "mercadopago", FORWARDING_FRONTEND, NO_CREDITCARD),
    AUTOPAY(54, "autopay", FORWARDING_FRONTEND, NO_CREDITCARD),
    CELLPAYPOINT(55, "cellpaypoint", FORWARDING_FRONTEND, NO_CREDITCARD),
    EBUCKS(56, "ebucks", FORWARDING_FRONTEND, NO_CREDITCARD),
    MIMONEY(57, "mimoney", FORWARDING_FRONTEND, NO_CREDITCARD),
    NEDBANK(58, "nedbank", FORWARDING_FRONTEND, NO_CREDITCARD),
    PAYU(59, "payu", FORWARDING_FRONTEND, NO_CREDITCARD),
    WIWALLET(60, "wiwallet", FORWARDING_FRONTEND, NO_CREDITCARD),
    BRADESCO(61, "bradesco", FORWARDING_FRONTEND, NO_CREDITCARD),
    YAPI_KREDI(62, new String[]{"yapikredi", "visa", "mc"}, null, NO_CREDITCARD),
    DANKORT(63, "dankort", null, IS_CREDITCARD),
    PAGOS_ONLINE(64, "pagosonline", null, NO_CREDITCARD),
    EPS(65, "eps", FORWARDING_FRONTEND, NO_CREDITCARD),
    INVOICE(66, "invoice", FORWARDING_FRONTEND, NO_CREDITCARD),
    DEBITCARDS_HPP(67, new String[]{"debitcard", "visa", "mc"}, FORWARDING_FRONTEND, NO_CREDITCARD),
    CASHU(68, "cashu", FORWARDING_FRONTEND, NO_CREDITCARD),
    SCBEASY(69, "scbeasy", FORWARDING_FRONTEND, NO_CREDITCARD),
    KTBONLINE(70, "ktbonline", FORWARDING_FRONTEND, NO_CREDITCARD),
    UOBDIRECTDEBIT(71, "uobdirectdebit", FORWARDING_FRONTEND, NO_CREDITCARD),
    KRUNGSRIONLINE(72, "krungsrionline", FORWARDING_FRONTEND, NO_CREDITCARD),
    BUALUANGIBANKING(73, "bualuangibanking", FORWARDING_FRONTEND, NO_CREDITCARD),
    TMBINTERNETBANKING(74, "tmbinternetbanking", FORWARDING_FRONTEND, NO_CREDITCARD),
    PAYTHRU_AMT(75, "paythru_amt", FORWARDING_FRONTEND, NO_CREDITCARD),
    MULTIPLUSPOINTS(76, "multipluspoints", null, NO_CREDITCARD),
    ELO(77, "elo", null, IS_CREDITCARD),
    SMARTMONEY(78, "smartmoney", FORWARDING_FRONTEND, NO_CREDITCARD),
    PAYTHRU_EFT(79, "paythru_eft", FORWARDING_FRONTEND, NO_CREDITCARD),
    ONETWOTHREE(80, new String[]{"onetwothree_atm", "onetwothree_bankcounter", "onetwothree_ibanking", "onetwothree_overthecounter"}, FORWARDING_FRONTEND, NO_CREDITCARD),
    TRUSTLY(81, "trustly", FORWARDING_FRONTEND, NO_CREDITCARD),
    MULTIBANCO(82, "multibanco", FORWARDING_FRONTEND, NO_CREDITCARD),
    SEPA_DIRECT_DEBIT(83, "sepadirectdebit", null, NO_CREDITCARD),
    BANK_TRANSFER(84, "bankTransfer", null, NO_CREDITCARD),
    BARRAS(85, "barras", null, NO_CREDITCARD),
    BALOTO(86, "baloto", null, NO_CREDITCARD),
    ACHCOLOMBIA(87, "achcolombia", null, NO_CREDITCARD),

    EMPTY(null, "", null, NO_CREDITCARD); // extra payment type for better caching of recurring payment infos (when were is no info object available we use this payment type)

    private final Integer id;
    private final String[] names;// under this names adyen supplies the payment method
    // a web frontend payment is some hpp style payment where we are not collecting the payment data and using a special api to transfer it,
    // but instead the payment service provider will collect the data directly from the customer and afterwards returns the customer back to us.
    private final WebFrontendType webFrontendType;

    private final CreditCardSchemaType creditCardSchema;

    PaymentType(final Integer id, final String name, final WebFrontendType webFrontendType, final CreditCardSchemaType creditCardSchema) {
        this(id, new String[]{name}, webFrontendType, creditCardSchema);
    }

    PaymentType(final Integer id, final String[] names, final WebFrontendType webFrontendType, final CreditCardSchemaType creditCardSchema) {
        this.id = id;
        this.names = names;
        this.webFrontendType = webFrontendType;
        this.creditCardSchema = creditCardSchema;
    }

    public Integer getId() {
        return this.id;
    }

    // Note! This will be used as the allowedMethods and brandCode parameters for HPP
    // See the section 2.9 of the HPP Integration Manual at https://www.adyen.com/home/support/manuals
    public String getName() {
        return this.names[0];
    }

    public static PaymentType getByName(final String name) {
        final Collection<PaymentType> result = Collections2.filter(Arrays.asList(PaymentType.values()), new Predicate<PaymentType>() {
            @Override
            public boolean apply(final PaymentType input) {
                return input.getName().equalsIgnoreCase(name);
            }
        });
        return result.size() == 1 ? result.iterator().next() : null;
    }

    public String[] getNames() {
        return Arrays.copyOf(this.names, this.names.length);
    }

    public Boolean isWebFrontendType() {
        return this.webFrontendType != null;
    }

    public WebFrontendType getWebFrontendType() {
        return this.webFrontendType;
    }

    public boolean isCreditCardSchema() {
        return creditCardSchema == IS_CREDITCARD;
    }

    public enum CreditCardSchemaType {
        IS_CREDITCARD, NO_CREDITCARD
    }
}
