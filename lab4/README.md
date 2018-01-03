# LAB 2:  Circuit Breaker Pattern

# Table of contents
1. [Introduction](#introduction)
2. [Circuit Breaker Pattern](#circuitbreakerpattern)
    1. [Hystrix](#histrix)
    2. [Turbine](#turbine)
    3. [Personne Accounts calls](#usecase)
3. [Account Microservice](#accountmsa)
    1. [Creating the application from template](#accountmsa-template)
    2. [Testing the application](#demodata)
4. [Personne Microservice with Hystrix dependency](#updatepersonne)
    1. [EnableCircuitBreaker and HystrixCommand ](#hystrixintegration)
    2. [Demo: Circuit closed](#democircuitclosed)
    3. [Demo: Circuit open](#democircuitopen)
5. [Next Labs](#next)


### Introduction <a name="introduction"></a>
In [Lab 1](../lab1/), you created an ephemeral MongoDB to store Microservices data, You also create various Kubernetes objects 
to build, deploy,expose your application.
In [Lab 2](../lab2/), you created a persitent database to store , you also created templates and quickly deploy your microservices
in a new environement.
In [Lab 3](../lab3/), you used jenkins2 pipelines to promote images from DEV to UAT namespace, automatically trigger application 
deployments.
Now we have a deed understanding of our micro services and we will be focusing on various patterns to make them more powerfull;
In the following lab4, we will focus on *Circuit Breaker pattern*.

---
### Circuit Breaker Pattern <a name="circuitbreakerpattern"></a>
<a href="https://martinfowler.com/bliki/CircuitBreaker.html">Circuit breaker pattern</a> was populariezd by Michael Nygard 
Circuit breaker pattern prevents cascade methods/functions  failures across distributed systems calls.
The basic idea behind the circuit breaker is to protected function call in a circuit breaker object, which monitors for failures. Once the failures reach a certain threshold, the circuit breaker object stop sending requests to the failed component, and open a new circuit that can be implemented by a fallback function. Once the failde component became available again, the circuit breaker close the opened circuit and automatically start sending new request to the target destination.

Regarding SpringBoot Microservices, applications can leverage <a href=https://cloud.spring.io/spring-cloud-netflix/> Spring Cloud Netflix</a> components ( hystrix and turbine)  to set up a Circuit Breaker pattern


lab4 will be implemented in a new openshift project  **circuit-breaker** 

```
 oc new-project circuit-breaker
```


#### Hystrix  <a name="histrix"></a>
 <a href=https://github.com/Netflix/Hystrix>Hystrix</a> is a library that helps application developpers to increase microservices resilience. 
* Isolate remote calls
* Stop cascade failures in distributed systems calls
* Provide call fallback options
* Add latency and  fault tolerance  logic

Follow the following instructions to install the Hystrix dashobard in the circruit-breaker project
* Install hysterix dashboard
```
$ oc create -f http://repo1.maven.org/maven2/io/fabric8/kubeflix/hystrix-dashboard/1.0.28/hystrix-dashboard-1.0.28-openshift.yml
 serviceaccount "ribbon" created
 service "hystrix-dashboard" created
 deploymentconfig "hystrix-dashboard" created
```
* Expose the hystrix dashboard
```
oc expose service hystrix-dashboard --port=8080
```



#### Turbine  <a name="turbine"></a>
<a href="https://github.com/Netflix/Turbine/wiki">turbine</a>  is a tool for aggregating streams of Server-Sent Event (SSE) JSON data into a single stream; it can be used as  hystrix stats collector.

Follow the following instructions to setup a turbine server in the project
* Create turbine Kubernetes objects
```
$ oc create -f http://repo1.maven.org/maven2/io/fabric8/kubeflix/turbine-server/1.0.28/turbine-server-1.0.28-openshift.yml
serviceaccount "turbine" created
service "turbine-server" created
deploymentconfig "turbine-server" created
```

* Grant the admin  role to the newly created turbine service account 
```
$ oc policy add-role-to-user admin  -z turbine
```
* Expose the turbine server

```
$ oc expose service turbine-server
route "turbine-server" exposed
```

You should have the following routes in your namespace at this stage
```
$ oc get routes
NAME                HOST/PORT                                                 PATH      SERVICES            PORT      TERMINATION
hystrix-dashboard   hystrix-dashboard-circuit-breaker.192.168.99.100.nip.io             hystrix-dashboard   8080
turbine-server      turbine-server-circuit-breaker.192.168.99.100.nip.io                turbine-server      http
```

![Hystrix and turbine pods](https://github.com/nelvadas/microservices-with-openshift/blob/master/lab4/hystrix-and-turbine-pods.png " Pods") 


#### Use Case  <a name="usecase"></a>



Notes
http://hystrix-dashboard-circuit-breaker.192.168.99.100.nip.io/monitor/monitor.html?stream=http%3A%2F%2Fturbine-server-circuit-breaker.192.168.99.100.nip.io%2Fturbine.stream


### Account List Microservice<a name="accountmsa"></a>

#### Creating application  <a name="accountmsa-template"></a>
To create the Account Micro service in the circuit-breaker namespace, you can use the following command template.

```
oc process https://raw.githubusercontent.com/nelvadas/microservices-with-openshift/master/lab4/accountapi-template-v1.0.0.json | oc create -f -
```
If you are using another namespace, you may need to edit the template first to match your specific parameters.
However You can also create the msa-account application from scratch step by step using the following instructions.
The two processes give the same results.

* Create the account MongoDB
```
oc new-app --docker-image=mongo:latest --name=accountdb
```

* Create the Account Microservice
```
oc new-app redhat-openjdk18-openshift~https://github.com/nelvadas/microservices-with-openshift.git            --context-dir=lab4/msa-account  --name=accountapi
```
If the imagestream is not present on you cluster you can use the following instruction to install it

```
oc create -f https://raw.githubusercontent.com/nelvadas/microservices-with-openshift/master/lab1/openjdk-s2i-imagestream.json -n \
openshift
```

* Create a configMap and mount it as volume to server application.properties
```
$ cd microservices-with-openshift/lab4/msa-account/configMap/dev
$ cat application.properties
spring.data.mongodb.host=accountdb
spring.data.mongodb.port=27017
$ oc create cm account-props-volume-cm --from-file=.
configmap "account-props-volume-cm" created
```

* Mount the configMap as volume on /deployment/config folder.
```
$ oc volume --add=true  --mount-path=/deployments/config --configmap-name=account-props-volume-cm --name=props-vol dc/accountapi 
deploymentconfig "accountapi" updated
```
The Account DB Application should be up and running 
```
$ oc get pods
NAME                        READY     STATUS      RESTARTS   AGE
accountapi-1-build          0/1       Completed   0          1d
accountapi-2-q2hrd          1/1       Running     0          1d
accountdb-1-ds8tf           1/1       Running     0          1d
hystrix-dashboard-1-6gcdd   1/1       Running     0          1d
turbine-server-1-1f1jx      1/1       Running     0          1d
```
* Expose the accountapi 
```
$ oc expose svc/accountapi
```
* Set probes
```
oc set probe dc/accountapi  --readiness  --initial-delay-seconds=5  --get-url=http://:8080/health
oc set probe dc/accountapi  --liveness  --initial-delay-seconds=0  --get-url=http://:8080/health
```

#### Testing the Application <a name="demodata"></a>

In this section, we are going to create a set of accounts for the user Number 1.
```
$ echo ' {"owner": "1", "accountType": "CompteCourant", "status" :"active", "balance": 10.00 }'| curl  'Content-Type: Application/json' -X POST -d @-  http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account/

Result: {"id":"5a4b2e29dc0e8200013fb122","owner":"1","accountType":"CompteCourant","creationDate":null,"balance":10.0,"status":"active"}
```

Run the following command to add more account to user N°1

```
echo ' {"owner": "1", "accountType": "LivretA", "status" :"active", "balance": 500.00 }'| curl -H 'Content-Type: Application/json' -X POST -d @-  http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account/



echo ' {"owner": "1", "accountType": "LDD", "status" :"active", "balance": 300.00 }'| curl -H 'Content-Type: Application/json' -X POST -d @-  http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account/
```

Check the account list for this user.





##  Personne Microservice with Hystrix dependency <a name="updatepersonne"></a>
#### EnableCircuitBreaker and HystrixCommand<a name="hystrixintegration"></a>
#### Demo: circuit closed <a name="democircuitclosed"></a>
#### Demo: circuit open <a name="democircuitopen"></a>

###  Next Steps <a name="next"></a>

* [Lab 5](../lab5/): Streaming with Apache Kafka

