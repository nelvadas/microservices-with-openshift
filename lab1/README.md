# LAB 1:  Building and Running a SpringBoot/MongoDB Microservice Application on Openshift using S2i. 

# Table of contents
1. [Introduction](#introduction)
2. [Application Setup](#appSetup)
    1. [S2I Builds](#s2i)
        1. [Database](#database)
        2. [Personne API](#personneApi)
    2. [Application Configuraiton](#config)
       1. [configMap](#configMap)
       2. [Properties Voumes ](#volumes)
       2. [DNS and Routing ](#routes)
3. [Running and Testing the Application](#testing)
4. [Becomes a kubernetes friend: probes](#probes)
5. [Next Labs](#next)


### Introduction <a name="introduction"></a>
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
### Application Setup <a name="appSetup"></a>
To setup the **personneapi** microservice application, we first need to be connected to a cluster 
login to you openshift cluster ( *our cluster IP is 192.168.99.100 and we are using the default developer account*)
```
oc login -u developer  -p developer https://192.168.99.100:8443
```

In this lab, we will be working only in the dev environment, so create a namespace **msa-dev** to keep the project

```
oc new-project msa-dev
```

#### S2i Builds <a name="s2i"></a>

##### Start a Mongo Database Pod <a name="database"></a>
The *personneapi* microservice relies on a MongDB to persist its data.

``` 
oc new-app --docker-image=mongo:latest --name=personnedb
```
As result, an *ephemeral* MongoDB service presonnedb is created and started  in the  msa-dev namespace.



##### Create the personneapi Microservice <a name="personneApi"></a>
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
An application pod is deployed with the result image 
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


#### Application Configuration <a name="config"></a>
In this section, we will provide a custom application.properties file to the application with custom details of our MongoDB instance.
Openshift defines a DNS convention to access services hosted on the cluster
```
<service>.<pod_namespace>.svc.cluster.local
```
So to acces the mongo instance in our namespace we can  use one of the following value as MongoDB IP/hostname: 
 ```
 personnedb.msa-dev.svc.cluster.local, personnedb.svc.cluster.local, personnedb
```


1. ConfigMap <a name="configMap"></a>

To pass the custom application.properties file, we can rely on a configMap and a volume to put the file in one SpringBoot expected location.

```
cd microservices-with-openshift/lab1/msa-personne
oc create cm props-volume-cm --from-file=./configMap/dev/
oc describe cm props-volume-cm

Name:		props-volume-cm
Namespace:	msa-dev
Labels:		<none>
Annotations:	<none>

Data
====
application.properties:
----
# Service DNS nommenclature
# <service>.<pod_namespace>.svc.cluster.local
spring.data.mongodb.host=personnedb.msa-dev.svc.cluster.local
spring.data.mongodb.port=27017

```

2. Volumes <a name="volume"></a>

 by default, Spring Boot will load application properties file from $JAR_FILE_LOCATION/config
 In this case the jar are deployed in /deployments folder, as consequences, application.properties file can be loaded from 
 /deployments/config without any other configuration.

To pass the custom application.propertie file from configMap to the application Pod filesystem, 
we need to create and mount volume with type=ConfigMap on /deployments/config with the configMap as content 
  

```
oc volume --add=true  --mount-path=/deployments/config --configmap-name=props-volume-cm --name=props-vol dc/personneapi
```
After running this command the deployment config is updated and the configuration change event occurs.
The application pod is redeployed with zero downtime ( Rolling Strategy) and the new version is bring live.

In application logs you can see the connection is open 

```
oc logs personneapi-4-30jbd
2017-11-28 01:00:57.969  INFO 1 --- [ter.local:27017] org.mongodb.driver.connection            : Opened connection [connectionId{localValue:1, serverValue:1}] to personnedb.msa-dev.svc.cluster.local:27017
2017-11-28 01:00:57.979  INFO 1 --- [ter.local:27017] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=personnedb.msa-dev.svc.cluster.local:27017, type=STANDALONE, state=CONNECTED, ok=true, version=ServerVersion{versionList=[3, 4, 10]}, minWireVersion=0, maxWireVersion=5, maxDocumentSize=16777216, roundTripTimeNanos=6752981}
2017-11-28 01:00:57.981  INFO 1 --- [ter.local:27017] org.mongodb.driver.cluster               : Discovered cluster type of STANDALONE
```

3. Routes <a name="routes"></a>

To be able to acces the personneapi microservices outside Openshift Cluster, we need a route to expose the associated service 

```
oc expose --path=/ svc/personneapi
```
This creates a route on which the application can be called outside Openshift Cluster
```
oc get routes
NAME          HOST/PORT                                   PATH      SERVICES      PORT       TERMINATION
personneapi   personneapi-msa-dev.192.168.99.100.nip.io   /         personneapi   8080-tcp
```

NB.
On some Minishift instances you need to update your resolv.conf to resolve the nip.io urls
```
sudo echo nameserver 8.8.8.8 > /etc/resolv.conf
```


### Application Tests  <a name="testing"></a>

The application is responding on personneapi route, by using a curl command we will perform the basic operations exposed by the microservice.

* Insert a new Foo/Bar Person
```
curl -X POST -H 'Content-Type: Application/json' -d '{ "ref" : "001", "firstName" : "Foo", "lastName" : "Baar", "birthDate": "1988-04-05T14:56:59.301Z", "customTag":"PoC" }' http://personneapi-msa-dev.192.168.99.100.nip.io/Personne/

{"ref":"001","firstName":"Foo","lastName":"Baar","birthDate":576255419301,"customTag":"PoC"}
```


* Find the inserted user
```
curl -H 'Content-Type: Application/json' http://personneapi-msa-dev.192.168.99.100.nip.io/Personne/001

Result: {"ref":"001","firstName":"Foo","lastName":"Baar","birthDate":576255419301,"customTag":"PoC"}
```
* Retrieve the collection of existing users
```
curl -H 'Content-Type: Application/json' http://personneapi-msa-dev.192.168.99.100.nip.io/Personne/
Result
[{"ref":"001","firstName":"Foo","lastName":"Baar","birthDate":576255419301,"customTag":"PoC"},{"ref":"002","firstName":"Test","lastName":"Tatampion","birthDate":null,"customTag":null}]
```

### Readiness and Liveness Probes <a name="probes"></a>
In order for k8s to determine if your application is ready to serve clients, you should define a readiness Probe 
* DB
The mongodb pod is ready when a client can open a tcp connection on port 27017

```
oc set probe dc/personnedb --readiness --open-tcp='27017'
```
* Personne API

Periodically k8s checks if your pod are still running ( Liveness probe)

```
oc set probe dc/personneapi  --readiness  --initial-delay-seconds=5  --get-url=http://:8080/health
oc set probe dc/personneapi  --liveness  --initial-delay-seconds=0  --get-url=http://:8080/health
```
give 5 sec to the application to start before sending the firt readiness check.

The /health URI is automatically configured in the spring boot application by adding the acurator in application pom.xml

```
     <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
```


### Next Steps <a name="introduction"></a>

* [Lab 2](../lab2/): Creating Reusable Application Templates
* [Lab 3](../lab3/): CI/CD with Jenkins2  Pipenlines

