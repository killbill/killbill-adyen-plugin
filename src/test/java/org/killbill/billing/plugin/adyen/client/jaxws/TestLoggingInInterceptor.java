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

package org.killbill.billing.plugin.adyen.client.jaxws;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLoggingInInterceptor {

    @Test(groups = "fast")
    public void testPayUCCNumObfuscation() {
        final String number = "5123456789012346";

        final String result = new LoggingInInterceptor().transform("          <entry>\n" +
                                                                   "            <key xsi:type=\"xsd:string\">PayU.ccnum</key>\n" +
                                                                   "            <value xsi:type=\"xsd:string\">" + number + "</value>\n" +
                                                                   "          </entry>\n");
        Assert.assertFalse(result.contains(number));
    }

    @Test(groups = "fast")
    public void testPayUCCVVObfuscation() {
        final String number = "111";

        final String result = new LoggingInInterceptor().transform("          <entry>\n" +
                                                                   "            <key xsi:type=\"xsd:string\">PayU.ccvv</key>\n" +
                                                                   "            <value xsi:type=\"xsd:string\">" + number + "</value>\n" +
                                                                   "          </entry>\n");
        Assert.assertFalse(result.contains(number));
    }

    @Test(groups = "fast")
    public void testPayUCCVVAndCCNumObfuscation() {
        final String ccvv = "111";
        final String ccnum = "5123456789012346";

        final String result = new LoggingInInterceptor().transform("          <entry>\n" +
                                                                   "            <key xsi:type=\"xsd:string\">PayU.ccvv</key>\n" +
                                                                   "            <value xsi:type=\"xsd:string\">" + ccvv + "</value>\n" +
                                                                   "          </entry>\n" +
                                                                   "          <entry>\n" +
                                                                   "            <key xsi:type=\"xsd:string\">PayU.ccnum</key>\n" +
                                                                   "            <value xsi:type=\"xsd:string\">" + ccnum + "</value>\n" +
                                                                   "          </entry>\n"
                                                                  );
        Assert.assertFalse(result.contains(ccvv));
        Assert.assertFalse(result.contains(ccnum));
    }

    @Test(groups = "fast")
    public void testPayUCCExpMonObfuscation() {
        final String number = "04";

        final String result = new LoggingInInterceptor().transform("          <entry>\n" +
                                                                   "            <key xsi:type=\"xsd:string\">PayU.ccexpmon</key>\n" +
                                                                   "            <value xsi:type=\"xsd:string\">" + number + "</value>\n" +
                                                                   "          </entry>\n");
        Assert.assertFalse(result.contains(number));
    }

    @Test(groups = "fast")
    public void testPayUCCExpYrObfuscation() {
        final String number = "2013";

        final String result = new LoggingInInterceptor().transform("          <entry>\n" +
                                                                   "            <key xsi:type=\"xsd:string\">PayU.ccexpyr</key>\n" +
                                                                   "            <value xsi:type=\"xsd:string\">" + number + "</value>\n" +
                                                                   "          </entry>\n");
        Assert.assertFalse(result.contains(number));
    }
}
