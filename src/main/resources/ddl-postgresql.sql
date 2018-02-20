/* We cannot use timestamp in MySQL because of the implicit TimeZone conversions it does behind the scenes */
CREATE DOMAIN datetime AS timestamp without time zone;

CREATE DOMAIN longtext AS text;
