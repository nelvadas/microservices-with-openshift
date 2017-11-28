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
login to you openshift cluster ( *our cluster IP is 192.168.99.100 and we are using the default developer account*)
```
oc login -u developer  -p developer https://192.168.99.100:8443
```

In this lab, we will be working only in the dev environment, so create a namespace **msa-dev** to keep the project

```
oc new-project msa-dev
```

#### S2i Builds

##### Start a Mongo Database Pod

```oc new-app --docker-image=mongo:latest --name=personnedb
--> Found Docker image d22888a (3 weeks old) from Docker Hub for "mongo:latest"

    * An image stream will be created as "personnedb:latest" that will track this image
    * This image will be deployed in deployment config "personnedb"
    * Port 27017/tcp will be load balanced by service "personnedb"
      * Other containers can access this service through the hostname "personnedb"
    * This image declares volumes and will default to use non-persistent, host-local storage.
      You can add persistent volumes later by running 'volume dc/personnedb --add ...'
    * WARNING: Image "mongo:latest" runs as the 'root' user which may not be permitted by your cluster administrator

--> Creating resources ...
    imagestream "personnedb" created
    deploymentconfig "personnedb" created
    service "personnedb" created
--> Success
    Run 'oc status' to view your app.
```
An ephemeral MongoDB service presonnedb is created and started  in the namespace msa-dev namespace.



##### Create the personneapi Microservice
Openshift provides a default S2i builder image to run Java JAR  applications: redhat-openjdk18-openshift
```oc get is -n openshift | grep jdk
redhat-openjdk18-openshift            registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift               latest,1.2,1.2-6 + 2 mo
```

Create the personneapi application with the following command



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

