/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen;

import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

public abstract class TestWithEmbeddedDBBase extends TestRemoteBase {

    protected AdyenDao dao;

    @BeforeSuite(groups = {"slow", "integration"})
    public void setUpBeforeSuite() throws Exception {
        EmbeddedDbHelper.instance().startDb();
    }

    @BeforeMethod(groups = {"slow", "integration"})
    public void setUpBeforeMethod() throws Exception {
        EmbeddedDbHelper.instance().resetDB();
        dao = EmbeddedDbHelper.instance().getAdyenDao();
    }

    @AfterSuite(groups = {"slow", "integration"})
    public void tearDownAfterSuite() throws Exception {
        EmbeddedDbHelper.instance().stopDB();
    }
}
