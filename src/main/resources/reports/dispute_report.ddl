CREATE TABLE `dispute_report` (
  `Company_Account` varchar(250) DEFAULT NULL,
  `Merchant_Account` varchar(250) DEFAULT NULL,
  `Psp_Reference` varchar(250) DEFAULT NULL,
  `Merchant_Reference` varchar(250) DEFAULT NULL,
  `Payment_Method` varchar(250) DEFAULT NULL,
  `Record_Date` datetime DEFAULT NULL,
  `Record_Date_TimeZone` varchar(250) DEFAULT NULL,
  `Dispute_Currency` varchar(250) DEFAULT NULL,
  `Dispute_Amount` varchar(250) DEFAULT NULL,
  `Record_Type` varchar(250) DEFAULT NULL,
  `Dispute_PSP_Reference` varchar(250) DEFAULT NULL,
  `Dispute_Reason` varchar(250) DEFAULT NULL,
  `RFI_Scheme_Code` varchar(250) DEFAULT NULL,
  `RFI_Reason_Code` varchar(250) DEFAULT NULL,
  `CB_Scheme_Code` varchar(250) DEFAULT NULL,
  `CB_Reason_Code` varchar(250) DEFAULT NULL,
  `Payment_Date` datetime DEFAULT NULL,
  `Payment_Date_TimeZone` varchar(250) DEFAULT NULL,
  `Payment_Currency` varchar(250) DEFAULT NULL,
  `Payment_Amount` varchar(250) DEFAULT NULL,
  `Dispute_Date` datetime DEFAULT NULL,
  `Dispute_Date_TimeZone` varchar(250) DEFAULT NULL,
  `Acquirer` varchar(250) DEFAULT NULL,
  `Dispute_ARN` varchar(250) DEFAULT NULL,
  `User_Name` varchar(250) DEFAULT NULL,
  `Risk_Scoring` varchar(250) DEFAULT NULL,
  `Shopper_Interaction` varchar(250) DEFAULT NULL,
  `Shopper_Name` varchar(250) DEFAULT NULL,
  `Shopper_Email` varchar(250) DEFAULT NULL,
  `Shopper_Reference` varchar(250) DEFAULT NULL,
  `Shopper_PAN` varchar(250) DEFAULT NULL,
  `Shopper_IP` varchar(250) DEFAULT NULL,
  `Shopper_Country` varchar(250) DEFAULT NULL,
  `Issuer_Country` varchar(250) DEFAULT NULL,
  `Issuer_Id` varchar(250) DEFAULT NULL,
  `3D_Directory_Response` varchar(250) DEFAULT NULL,
  `3D_Authentication_Response` varchar(250) DEFAULT NULL,
  `CVC2_Response` varchar(250) DEFAULT NULL,
  `AVS_Response` varchar(250) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX dispute_report_acct_date_pm ON dispute_report(Merchant_Account, Record_Date, Payment_Method);

/*
 * Copyright 2020-2020 Equinix, Inc
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

/* LOAD DATA INFILE '/tmp/dispute_report_YYYY_MM_DD.csv' INTO TABLE dispute_report FIELDS TERMINATED by ',' OPTIONALLY ENCLOSED BY '"' IGNORE 1 LINES; */
