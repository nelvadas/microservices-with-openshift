# Instructions
the msa-customer microserivce  aggregates customer  details
*  Nominal informations from msa-personne
 * Account list from msa-accounts

The msa-customer uses hystrix command to isolates msa-account
if the msa-account ws is not available, the msa-customer return the default user account.

Run the application on command line using 

java -jar -Dspring.config.location=file:/tmp/mongo/application.properties target/msa-customer-1.0.0.jar

A template of application.properties is provided in 

src/test/resources/application-template.properties
