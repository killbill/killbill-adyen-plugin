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

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestIgnoreUnexpectedElementsEventHandler {

    @Test(groups = "fast")
    public void testHandleEventIgnoresUnexpectedElements() throws Exception {
        final IgnoreUnexpectedElementsEventHandler eventHandler = new IgnoreUnexpectedElementsEventHandler();
        final ValidationEvent validationEvent = createValidationEvent(true);
        Assert.assertTrue(eventHandler.handleEvent(validationEvent), "Unexpected Element Validation Event is not ignored");
    }

    @Test(groups = "fast")
    public void testHandleEventDoesNotIgnoreOtherValidationErrors() throws Exception {
        final IgnoreUnexpectedElementsEventHandler eventHandler = new IgnoreUnexpectedElementsEventHandler();
        final ValidationEvent validationEvent = createValidationEvent(false);
        Assert.assertFalse(eventHandler.handleEvent(validationEvent), "other Validation Event is ignored");
    }

    private ValidationEvent createValidationEvent(final boolean isUnexpectedElementsMessage) {
        return new ValidationEvent() {
            @Override
            public int getSeverity() {
                throw new UnsupportedOperationException("getSeverity is not supported by ");
            }

            @Override
            public String getMessage() {
                return isUnexpectedElementsMessage ? "unexpected element ( tralala" : "other exception";
            }

            @Override
            public Throwable getLinkedException() {
                throw new UnsupportedOperationException("getLinkedException is not supported by ");
            }

            @Override
            public ValidationEventLocator getLocator() {
                throw new UnsupportedOperationException("getLocator is not supported by ");
            }
        };
    }
}
