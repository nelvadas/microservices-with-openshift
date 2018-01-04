# LAB 2:  Circuit Breaker Pattern

# Table of contents
1. [Introduction](#introduction)
2. [Circuit Breaker Pattern](#circuitbreakerpattern)
    1. [Definition](#definition)
    2. [Use Case ](#usecase)
    3. [Circuit Breaker implementation: state of the art](#circuitbreakerimpl)
       1. [Hystrix](#histrix)
       2. [Turbine](#turbine)
       3. [Istio and Envoy Proxy](#istio)
3. [Account Microservice](#accountmsa)
   1. [Creating the application from template](#accountmsa-template)
   2. [Testing the application](#demodata)
4. [Customer Microservice with Hystrix dependency](#updatepersonne)
    1. [EnableCircuitBreaker and HystrixCommand ](#hystrixintegration)
    2. [Deployment on Openshift](#openshiftdeployment)
    3. [Demo: Circuit closed](#democircuitclosed)
    4. [Demo: Circuit open](#democircuitopen)
5. [Next Labs](#next)


## Introduction <a name="introduction"></a>
In [Lab 1](../lab1/), you created an ephemeral MongoDB to store Microservices data, You also create various Kubernetes objects 
to build, deploy,expose your application.
In [Lab 2](../lab2/), you created a persitent database to store , you also created templates and quickly deploy your microservices
in a new environement.
In [Lab 3](../lab3/), you used jenkins2 pipelines to promote images from DEV to UAT namespace, automatically trigger application 
deployments.
Now we have a deed understanding of our micro services and we will be focusing on various patterns to make them more powerfull;
In the following lab4, we will focus on *Circuit Breaker pattern*.

---
## Circuit Breaker Pattern <a name="circuitbreakerpattern"></a>
###[Definition ](#definition)
<a href="https://martinfowler.com/bliki/CircuitBreaker.html">Circuit breaker pattern</a> was populariezd by Michael Nygard 
Circuit breaker pattern prevents cascade methods/functions  failures across distributed systems calls.
The basic idea behind the circuit breaker is to protected function call in a circuit breaker object, which monitors for failures. Once the failures reach a certain threshold, the circuit breaker object stop sending requests to the failed component, and open a new circuit that can be implemented by a fallback function. Once the failde component became available again, the circuit breaker close the opened circuit and automatically start sending new request to the target destination.

Regarding SpringBoot Microservices, applications can leverage <a href=https://cloud.spring.io/spring-cloud-netflix/> Spring Cloud Netflix</a> components ( hystrix and turbine)  to set up a Circuit Breaker pattern
###[Use Case ](#usecase)

lab4 will be implemented in a new openshift project  **circuit-breaker** 

```
 oc new-project circuit-breaker
```

### [Circuit Breaker implementation: state of the art](#circuitbreakerimpl)

When it comes to Circuit breaker implementations, the following frameworks and technologies can be considered
* Hystrix and Turbine
* Istio

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
Turbine is important if you want to aggregate a set of hystrix streams in a single hystrix dashboard but also when you hare working in a HA environement where services s
should be scaled up.

Turbine will collect  streams of available and autorized namesapces services and pass them to the single Hystrix dashboard


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

![Hystrix and turbine pods](https://github.com/nelvadas/microservices-with-openshift/blob/master/lab4/images/hystrix-and-turbine-pods.png " Pods") 


#### [Istio and Envoy Proxy](#istio)

In this lab we will only focus on Hystrix and Turbine for circuit breaking implementation and monitoring. Istio will be handle latter in another lab.


## Account Microservice<a name="accountmsa"></a>

### Creating the application from template<a name="accountmsa-template"></a>
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

### Testing the Application <a name="demodata"></a>

In this section, we are going to create a set of accounts for the user Number 1.
```
$ echo ' {"owner": "1", "accountType": "CompteCourant", "status" :"active", "balance": 10.00 }'| curl  'Content-Type: Application/json' -X POST -d @-  http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account/

Result: {"id":"5a4b2e29dc0e8200013fb122","owner":"1","accountType":"CompteCourant","creationDate":null,"balance":10.0,"status":"active"}
```

Run the following command to add more account to user N°1

```
echo ' {"owner": "1", "accountType": "LivretA", "status" :"active", "balance": 500.00 }'| curl -H 'Content-Type: Application/json' -X POST -d @-  http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account/



echo ' {"owner": "1", "accountType": "LDD", "status" :"active", "balance": 300.00 }'| curl -H 'Content-Type: Application/json' -X POST -d @-  http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account/

Add an account for user N°2

$ echo ' {"owner": "2", "accountType": "CompteCourant", "status" :"active", "balance": 15.60 }'| curl  'Content-Type: Application/json'     -X POST -d @-  http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account/


```

Check the account list for this user.

```
$ curl  http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account/1
HTTP/1.1 200
Cache-control: private
Content-Type: application/json;charset=UTF-8
Date: Tue, 02 Jan 2018 07:09:33 GMT
Set-Cookie: 7a7e76b0d8a493826cc96ebc81499590=72bb996b6478da2c918265aee1c6501e; path=/; HttpOnly
Transfer-Encoding: chunked
X-Application-Context: application

[
    {
        "accountType": "CompteCourant",
        "balance": 10.0,
        "creationDate": null,
        "id": "5a4b2e29dc0e8200013fb122",
        "owner": "1",
        "status": "active"
    },
    {
        "accountType": "LivretA",
        "balance": 500.0,
        "creationDate": null,
        "id": "5a4b2e9adc0e8200013fb123",
        "owner": "1",
        "status": "active"
    },
    {
        "accountType": "LDD",
        "balance": 300.0,
        "creationDate": null,
        "id": "5a4b2ea7dc0e8200013fb124",
        "owner": "1",
        "status": "active"
    }
]
```

The dependent account microservice is now ready, In the next section we will implement and setup a customer microservice with a dependency on msa-acount.


##  Customer  Microservice with Hystrix dependency <a name="updatepersonne"></a>

The customer Microserice agrgregate details from both msa-personne and msa-account to provide a single customer view.
We will implement remote calls to msa-personne and msa-account using Circuit breaker pattern so that whenenver the msa-account or msa-personne 
becomes unavailable, the msa-customer relies on local function calls to provide a fallback and avoid cascade failures.

### EnableCircuitBreaker and HystrixCommand<a name="hystrixintegration"></a>
There a few steps to include enable Circuit breaker in the Customer micro servcies.

1. In the pom file include the spring cloud hystrix dependency
```
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-hystrix</artifactId>
</dependency>
```
Also make sure the spring-boot-starter-actuator is present in your pom file.

2. Enable circuit breaker in your Controllers
<pre><code>
@RestController
<b>@EnableCircuitBreaker</b>
@RequestMapping("/Customer")

public class CustomerController {
	
	
	
	@Autowired
	private AccountService accountService;
	
	@Autowired
	private PersonneService personneService;
	...
</code></pre>

3. Use HystrixCommand and fallback in your service calls

<pre><code>
@Service
public class AccountService {

	@Value( "${account.svc.url}" )
	private String accountSvcBaseUrl;
	
    <b>@HystrixCommand(fallbackMethod="getCustomerAccountListDegrade")</b>
   <b> public List<Account> getCustomerAccountList(String id){</b>
    	System.out.println("account url="+accountSvcBaseUrl);
    	RestTemplate restTemplate = new RestTemplate();
    	URI accountUri = URI.create(String.format("%s/%s", accountSvcBaseUrl,id));
    	Account[] userAccounts= restTemplate.getForObject(accountUri, Account[].class);
    	return (List<Account>)Arrays.asList(userAccounts);
    }
    
    
    
<b>	public List<Account> getCustomerAccountListDegrade(String id){  </b>
    	 Account userDefaultAccount = new Account();
    	 userDefaultAccount.setAccountType("CompteCourant");
    	 userDefaultAccount.setOwner(id);
    	 userDefaultAccount.setStatus("*****ServiceDégradé****");
    	 userDefaultAccount.setBalance(1.00);
    	 return (List<Account>)Arrays.asList(userDefaultAccount);
    	 
    }
	
</code></pre>
	
4. (Optionnal) Adjust your Hystrix configuration in application.properties
```
MacBook-Pro-de-elvadas:dev enonowog$ cat application.properties
account.svc.url=http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account
personne.svc.url=http://personneapi-msa-dev.192.168.99.100.nip.io/Personne/

#Hystrix configuration
hystrix.metrics.enabled=true
management.security.enabled=false
endpoints.health.sensitive=false
```


### Deployment in Openshift<a name="openshiftdeployment"></a>
* Create the CustomerAPI application
```
oc new-app redhat-openjdk18-openshift~https://github.com/nelvadas/microservices-with-openshift.git --context-dir=lab4/msa-customer \
  --name=customerapi
```


* Prepare the  rigth application.properties file to supply configMap
``` 
$ cd microservices-with-openshift/lab4/msa-customer/configMap/dev
$ cat application.properties
  account.svc.url=http://accountapi-circuit-breaker.192.168.99.100.nip.io/Account
  personne.svc.url=http://personneapi-msa-dev.192.168.99.100.nip.io/Personne/
```

* Create the msa-customer-props-volume-cm
```
$ oc create cm msa-customer-props-volume-cm --from-file=.
configmap "msa-customer-props-volume-cm" created
```

* Mount a volume on /deployments/config
```
oc volume --add=true  --mount-path=/deployments/config --configmap-name=msa-customer-props-volume-cm --name=props-vol dc/customerapi
```
* Expose the customerapi service
```
oc expose svc/customerapi
```

* The customer microserice exposes an hystrix stream at 
```
$ curl http://customerapi-circuit-breaker.192.168.99.100.nip.io/hystrix.stream
...

```
* To simulate two hosts, scale the hystrix to 02 replicas
```
$ oc scale dc/customerapi --replicas=2
```

* In order for Turbine to be aware of the customerapi hystrix streams, the service should be annotated with the label hystrix.enabled=true
```
oc label svc/customerapi hystrix.enabled=true
```
We can also add a specific hystrix cluster name; by default all the hystrix streams will be agregated in a single default cluster.
```
oc label svc/customerapi hystrix.cluster=default
```
once you complete this operation, your turbine server should detect the hystrix endpoints for all the customerapi pods

```
$ curl http://turbine-server-circuit-breaker.192.168.99.100.nip.io/discovery

<h1>Hystrix Endpoints:</h1>
<h3>http://172.17.0.5:8080/hystrix.stream default:true</h3>
<h3>http://172.17.0.9:8080/hystrix.stream default:true</h3>

```

Turbine stream should be available from *http://turbine-server-circuit-breaker.192.168.99.100.nip.io/turbine.stream*


```
$ curl   http://turbine-server-circuit-breaker.192.168.99.100.nip.io/turbine.stream
: ping
data: {"currentCorePoolSize":10,"currentLargestPoolSize":10,"propertyValue_metricsRollingStatisticalWindowInMilliseconds":10000,"currentActiveCount":0,"currentMaximumPoolSize":10,"currentQueueSize":0,"type":"HystrixThreadPool","currentTaskCount":200,"currentCompletedTaskCount":200,"rollingMaxActiveThreads":0,"rollingCountCommandRejections":0,"name":"AccountService","reportingHosts":1,"currentPoolSize":10,"propertyValue_queueSizeRejectionThreshold":5,"rollingCountThreadsExecuted":0}

data: {"rollingCountFallbackSuccess":0,"rollingCountFallbackFailure":0,"propertyValue_circuitBreakerRequestVolumeThreshold":20,"propertyValue_circuitBreakerForceOpen":false,"propertyValue_metricsRollingStatisticalWindowInMilliseconds":10000,"latencyTotal_mean":0,"rollingMaxConcurrentExecutionCount":0,"type":"HystrixCommand","rollingCountResponsesFromCache":0,"rollingCountBadRequests":0,"rollingCountTimeout":0,"propertyValue_executionIsolationStrategy":"THREAD","rollingCountFailure":0,"rollingCountExceptionsThrown":0,"rollingCountFallbackMissing":0,"threadPool":"AccountService","latencyExecute_mean":0,"isCircuitBreakerOpen":false,"errorCount":0,"rollingCountSemaphoreRejected":0,"group":"AccountService","latencyTotal":{"0":0,"99":0,"100":0,"25":0,"90":0,"50":0,"95":0,"99.5":0,"75":0},"requestCount":0,"rollingCountCollapsedRequests":0,"rollingCountShortCircuited":0,"propertyValue_circuitBreakerSleepWindowInMilliseconds":5000,"latencyExecute":{"0":0,"99":0,"100":0,"25":0,"90":0,"50":0,"95":0,"99.5":0,"75":0},"rollingCountEmit":0,"currentConcurrentExecutionCount":0,"propertyValue_executionIsolationSemaphoreMaxConcurrentRequests":10,"errorPercentage":0,"rollingCountThreadPoolRejected":0,"propertyValue_circuitBreakerEnabled":true,"propertyValue_executionIsolationThreadInterruptOnTimeout":true,"propertyValue_requestCacheEnabled":true,"rollingCountFallbackRejection":0,"propertyValue_requestLogEnabled":true,"rollingCountFallbackEmit":0,"rollingCountSuccess":0,"propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests":10,"propertyValue_circuitBreakerErrorThresholdPercentage":50,"propertyValue_circuitBreakerForceClosed":false,"name":"getCustomerAccountList","reportingHosts":1,"propertyValue_executionIsolationThreadPoolKeyOverride":"null","propertyValue_executionIsolationThreadTimeoutInMilliseconds":1000,"propertyValue_executionTimeoutInMilliseconds":1000}

...

```


### Demo: circuit closed <a name="democircuitclosed"></a>
The circuit is said to be closed when the hystrix command call is  using the primary service call , not the fallback.

Check all your services  are up and running.
```
$ oc get pods
NAME                        READY     STATUS    RESTARTS   AGE
accountapi-5-sfmvg          1/1       Running   2          1d
accountdb-1-ds8tf           1/1       Running   1          2d
customerapi-3-bn9k9         1/1       Running   0          4m
customerapi-3-zmfps         1/1       Running   0          3m
hystrix-dashboard-1-6gcdd   1/1       Running   1          2d
turbine-server-15-kwh2p     1/1       Running   1          1d
```


Open the hystrix dashboard by clicking on <a href="http://hystrix-dashboard-circuit-breaker.192.168.99.100.nip.io">http://hystrix-dashboard-circuit-breaker.192.168.99.100.nip.io</a>

Enter the turbine stream <a href="http://turbine-server-circuit-breaker.192.168.99.100.nip.io/turbine.stream">http://turbine-server-circuit-breaker.192.168.99.100.nip.io/turbine.stream</a> to monitor the whole services.


![Hystrix Dashboard](https://github.com/nelvadas/microservices-with-openshift/blob/master/lab4/images/hystrix-dashboard.png " Dashboard")


To monitor a specific hystrix stream, you can use the associated hystrix url : eg  http://customerapi-circuit-breaker.192.168.99.100.nip.io/hystrix.stream for customerapi


```
 $ for i in {1..100}  ;do http  http://customerapi-circuit-breaker.192.168.99.100.nip.io/Customer/1  ; done
```
you may also want to use the *ab* command to send a huge load concurrently.
```
$ ab -n 100 http://customerapi-circuit-breaker.192.168.99.100.nip.io/Customer/1
```

As we did not plug a personneapi service, in a normal working behaviour is to have the following details for Customer N°1

```

HTTP/1.1 200
Cache-control: private
Content-Type: application/json;charset=UTF-8
Date: Tue, 02 Jan 2018 17:54:24 GMT
Set-Cookie: 89d8f9242644e4d50e3d4fc0cb57fb52=b8cddb30aae526ceebb54e89f5177dd9; path=/; HttpOnly
Transfer-Encoding: chunked
X-Application-Context: application

{
    "accounts": [
        {
            "accountType": "CompteCourant",
            "balance": 10.0,
            "creationDate": null,
            "id": "5a4b2e29dc0e8200013fb122",
            "owner": "1",
            "status": "active"
        },
        {
            "accountType": "LivretA",
            "balance": 500.0,
            "creationDate": null,
            "id": "5a4b2e9adc0e8200013fb123",
            "owner": "1",
            "status": "active"
        },
        {
            "accountType": "LDD",
            "balance": 300.0,
            "creationDate": null,
            "id": "5a4b2ea7dc0e8200013fb124",
            "owner": "1",
            "status": "active"
        }
    ],
    "identity": {
        "birthDate": null,
        "customTag": "*****ServicePersonneDégradé****",
        "firstName": null,
        "lastName": null,
        "ref": "1"
    }
}
```
In this configuration, the AccountService is responding fine. ( Circuit Closed)
At the contrary, the PersonneService is using the fallback to return the default Identify
The diagram show ( Hosts=2) as we have two pods for the customerapi service.

![Circuit Closed](https://github.com/nelvadas/microservices-with-openshift/blob/master/lab4/images/hystrix-circuitclosed.png " Circuit Closed")


### Demo: circuit open <a name="democircuitopen"></a>
Whenever the primary path defined for the remote ws become unreachable ( min thresold=20 calls per window frame : 1min) 
the circuit is opened.

Scale the accountapi down to make fail all the requests to the getCustomerAccountList

```
$ oc scale dc/accountapi --replicas=0
```

The account service becomes unavailable, so Hystrix command should fall back on the second route
to return the default user account as implemented in the AccountService class.

```
$ ab -n 100 http://customerapi-circuit-breaker.192.168.99.100.nip.io/Customer/1

HTTP/1.1 200
Cache-control: private
Content-Type: application/json;charset=UTF-8
Date: Tue, 02 Jan 2018 18:05:45 GMT
Set-Cookie: 89d8f9242644e4d50e3d4fc0cb57fb52=c6482b502dea1a3307fec95f1d46821c; path=/; HttpOnly
Transfer-Encoding: chunked
X-Application-Context: application

{
    "accounts": [
        {
            "accountType": "CompteCourant",
            "balance": 1.0,
            "creationDate": null,
            "id": null,
            "owner": "1",
            "status": "*****ServiceDégradé****"
        }
    ],
    "identity": {
        "birthDate": null,
        "customTag": "*****ServicePersonneDégradé****",
        "firstName": null,
        "lastName": null,
        "ref": "1"
    }
}

```
The circuit is open progressively as request are handled by pods.


![PartiallyOpen](https://github.com/nelvadas/microservices-with-openshift/blob/master/lab4/images/hystrix-circuit-pod-open.png "PartiallyOpen")

 At the end the circuit is opened for all the pods and transition to the following state

![PartiallyOpen](https://github.com/nelvadas/microservices-with-openshift/blob/master/lab4/images/hystrix-circuit-open.png "PartiallyOpen")


##  Next Steps <a name="next"></a>

* [Lab 5](../lab5/): Streaming with Apache Kafka

