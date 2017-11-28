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
To setup the **personneapi** microservice application, we first need to be connected to a cluster 
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
The *personneapi* microservice relies on a MongDB to persist its data.

``` 
oc new-app --docker-image=mongo:latest --name=personnedb
```
As result, an *ephemeral* MongoDB service presonnedb is created and started  in the  msa-dev namespace.



##### Create the personneapi Microservice
Openshift provides a default S2i builder image to run JAR  applications: redhat-openjdk18-openshift

```
oc get is -n openshift | grep jdk
redhat-openjdk18-openshift   registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift    latest,1.2,1.2-6 + 2 mo
```

Create the personneapi application with the following command

```
oc new-app redhat-openjdk18-openshift:1.2-6~https://github.com/nelvadas/microservices-with-openshift.git \
           --context-dir=lab1/msa-personne --name=personneapi
```

a build pod is created to checkout and build source code from the specified Git repository
```
$ oc get pods
NAME                  READY     STATUS    RESTARTS   AGE
personneapi-1-build   1/1       Running   0          20h
personnedb-1-zl6l1    1/1       Running   0          23h
```
Check the build logs ( -f => follow  ) 
```
$ oc logs -f personneapi-1-build
```

Once the build completed, the  jar is copied in the /deployments folder and the resulting application image is
 pushed in the docker registry.
 
```
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 10:26 min
[INFO] Finished at: 2017-11-27T23:19:45+00:00
[INFO] Final Memory: 28M/59M
[INFO] ------------------------------------------------------------------------
[WARNING] The requested profile "openshift" could not be activated because it does not exist.
Copying Maven artifacts from /tmp/src/target to /deployments ...
Running: cp *.jar /deployments
... done
Pushing image 172.30.1.1:5000/msa-dev/personneapi:latest ...
Pushed 0/6 layers, 1% complete
Pushed 1/6 layers, 26% complete
Pushed 2/6 layers, 41% complete
Pushed 3/6 layers, 71% complete
Pushed 4/6 layers, 81% complete
Pushed 5/6 layers, 91% complete
Pushed 6/6 layers, 100% complete
Push successful
```

When The build pod completes  an application pod is deployed with the result image 
```
$ oc get pods
NAME                  READY     STATUS      RESTARTS   AGE
personneapi-1-bmnfk   1/1       Running     0          20h
personneapi-1-build   0/1       Completed   0          20h
personnedb-1-zl6l1    1/1       Running     0          23h
``` 

The *personneapi-1-bmnfk* pod  is running but the configuration is not yet fine,
 you can see in pod logs that the application failed to open a database connection since the app pick the default application.properties
embebed in the jar file.

```
2017-11-27 23:20:16.916  INFO 1 --- [localhost:27017] org.mongodb.driver.cluster               : Exception in monitor thread while connecting to server localhost:27017
com.mongodb.MongoSocketOpenException: Exception opening socket
	at com.mongodb.connection.SocketStream.open(SocketStream.java:63) ~[mongodb-driver-core-3.4.3.jar!/:na]
	at com.mongodb.connection.InternalStreamConnection.open(InternalStreamConnection.java:115) ~[mongodb-driver-core-3.4.3.jar!/:na]
	at com.mongodb.connection.DefaultServerMonitor$ServerMonitorRunnable.run(DefaultServerMonitor.java:113) ~[mongodb-driver-core-3.4.3.jar!/:na]
	at java.lang.Thread.run(Thread.java:748) [na:1.8.0_151]
Caused by: java.net.ConnectException: Connection refused (Connection refused)
	at java.net.PlainSocketImpl.socketConnect(Native Method) ~[na:1.8.0_151]
	at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:350) ~[na:1.8.0_151]
	at java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:206) ~[na:1.8.0_151]
	at java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:188) ~[na:1.8.0_151]
	at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:392) ~[na:1.8.0_151]
	at java.net.Socket.connect(Socket.java:589) ~[na:1.8.0_151]
	at com.mongodb.connection.SocketStreamHelper.initialize(SocketStreamHelper.java:57) ~[mongodb-driver-core-3.4.3.jar!/:na]
	at com.mongodb.connection.SocketStream.open(SocketStream.java:58) ~[mongodb-driver-core-3.4.3.jar!/:na]
	... 3 common frames omitted
```

In the next section we will update the application configuration and provide a correct configuration file with database parameters.


#### Application Configuration
In this section, we will provide a custom application.properties file to the application with custom details of our MongoDB instance.
Openshift defines a DNS convention to access services hosted on the cluster
```
<service>.<pod_namespace>.svc.cluster.local
```
So to acces the mongo instance in our namespace we should use one of the following  hostname: 
* ```
 personnedb.msa-dev.svc.cluster.local
```
* ```
personnedb.svc.cluster.local
```
*  ```
personnedb
```


1. ConfigMap
To pass the custom application.properties file, we can rely on a configMap and a volume to put the file in one SpringBoot expected location.

```
cd microservices-with-openshift/lab1/msa-personne

cat configMap/dev/application.properties
   spring.data.mongodb.host=personnedb.msa-dev.svc.cluster.local
   spring.data.mongodb.port=27017

oc create cm props-volume-cm —from-file=./configMap/dev/

```

2. Volumes





3. Routes


#### Readiness and Liveness Probes

#### Application Tests



### Next Steps

The lab series is organized around the folowing items

* [Lab 2](../lab2/): Creating Reusable Application Templates
* [Lab 3](../lab3/): CI/CD with Jenkins2  Pipenlines

