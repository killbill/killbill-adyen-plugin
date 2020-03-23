/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.payment.service.checkout;

import java.util.ArrayList;
import java.util.List;
import com.adyen.model.checkout.CheckoutPaymentsAction;
import com.adyen.model.checkout.CheckoutPaymentsAction.CheckoutActionType;
import com.adyen.model.checkout.InputDetail;
import com.adyen.model.checkout.PaymentsResponse;
import com.adyen.model.checkout.PaymentsResponse.ResultCodeEnum;

public abstract class CheckoutApiTestHelper {
    public static String URL = "https://checkout-test.adyen.com/checkoutPaymentRedirect?redirectData=abcdef12345";
    public static String PAYMENT_DATA = "abcde12345";
    public static String PSP_REFERENCE = "853584459070452C";

    public static PaymentsResponse getRedirectShopperResponse() {
        List<InputDetail> details = new ArrayList<InputDetail>();
        InputDetail returnKey = new InputDetail();
        returnKey.setKey("redirectResult");
        returnKey.setType("text");
        details.add(returnKey);

        CheckoutPaymentsAction action = new CheckoutPaymentsAction();
        action.setPaymentData(PAYMENT_DATA);
        action.setMethod("GET");
        action.setType(CheckoutActionType.REDIRECT);
        action.setPaymentMethodType("klarna");
        action.setUrl(URL);

        PaymentsResponse authoriseResponse = new PaymentsResponse();
        authoriseResponse.setResultCode(ResultCodeEnum.REDIRECTSHOPPER);
        authoriseResponse.setDetails(details);
        authoriseResponse.setAction(action);
        return authoriseResponse;
    }

    public static PaymentsResponse getAuthorisedResponse() {
        PaymentsResponse authoriseResponse = new PaymentsResponse();
        authoriseResponse.setResultCode(ResultCodeEnum.AUTHORISED);
        authoriseResponse.setPspReference(PSP_REFERENCE);
        return authoriseResponse;
    }
}
