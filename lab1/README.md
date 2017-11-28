## LAB 1:  Building and Running a SpringBoot/MongoDB Microservice Application on Openshift using S2i. 


### Introduction
The purpose of this lab is to build a microservice on Openshift using the S2i process
To keep it simple, the  msa-personne application model has only one Entity persisted in a MongoDB document 

``` 
  Personne(ref, firstName, lastName, birthDate..)
  e.g  {"ref":"001","firstName":"Foo","lastName":"Bar","birthDate":} 
```

This application will expose three main endpoints to server the following functionnalities
*  ```GET /personne/{ref} ```   To retreive a user details based on a reference
*  ```GET /personne/ ```        Retreive all users
*  ```POST /personne/ ```       Insert new user document in MongoDB database


### Application Setup

#### S2i Builds

#### Application Configuration

#### Application Tests



### Next Steps

The lab series is organized around the folowing items

* [LAB2](../lab2/): Creating Reusable Application Templates
* [LAB6](../lab3/): CI/CD with Jenkins2  Pipenlines

