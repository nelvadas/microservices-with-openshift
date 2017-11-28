## LAB 1:  Building and Running a SpringBoot/MongoDB Microservice Application on Openshift using S2i. 


### Introduction
The purpose of this lab is to build a microservice on Openshift using the S2i process.
To keep it simple, the  msa-personne application model has only one Entity persisted in a MongoDB document 

``` 
  Personne(ref, firstName, lastName, birthDate..)
  e.g  {"ref":"001","firstName":"Foo","lastName":"Bar","birthDate":} 
```

This application will expose three main endpoints to server the following functionnalities
*  ```GET /Personne/{ref} ```   To retreive a user details based on a reference
*  ```GET /Personne/ ```        Retreive all items from Personne Collection
*  ```POST /Personne/ ```       Insert new user document in MongoDB 

---
### Application Setup
To setup the microservice application, we first need to be connected to a cluster 
login to you openshift cluster ( our cluster IP is 192.168.99.100)
```
oc login -u developer https://192.168.99.100:8443
```
The default developer password is developper on a Minishift cluster.

Create a namespace to hold the project, In  [Lab3] (../lab3) we will see how to promote projects from one environement(dev) to another ( uat or prod)
In this lab we are working only on the dev environment

```
oc new-project msa-dev
```

#### S2i Builds

#### Readiness and Liveness Probes


#### Application Configuration

1. ConfigMap
2. Volumes
3. Routes


#### Application Tests



### Next Steps

The lab series is organized around the folowing items

* [Lab 2](../lab2/): Creating Reusable Application Templates
* [Lab 3](../lab3/): CI/CD with Jenkins2  Pipenlines

