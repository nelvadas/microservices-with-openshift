## Microservices with Openshift 

The purpose of this lab series is to demonstrate how to start a microservice journey with Openshift using the following technologies
* Openshift 3.6+
* SpringBoot
* MongoDB
* Jenkins2
* Ansible 

### Pre-requisites

 It is assumed that you have an OpenShift cluster instance running and available. This instance can take several forms dependin    g on your environment and needs :
 * Full blown OpenShift cluster at your site, see how to [Install OpenShift at your site](https://docs.openshift.com/container-platform/3.6/install_config/index.html), 
 * Red Hat Container Development Kit on your laptop, see how to [Get Started with CDK](http://developers.redhat.com/products/cdk/get-started/),
  * Lightweight Minishift on your laptop, see [Minishift project page](https://github.com/minishift/minishift).
 You should also have the `oc` client line interface tool installed on your machine. Pick the corresponding OpenShift version from [this     page](https://github.com/openshift/origin/releases).
 
The following labs are completed and tested on an Openshift 3.6

### Agenda

The lab series is organized around the folowing items

* [Lab 1](./lab1/): Building and Running  a SpringBoot/MongoDB Microservice Application on Openshift using S2i.
* [Lab 2](./lab2/): Creating Persistent Databases and  Reusable Application Templates
* [Lab 3](./lab3/): Continuous Integration and Delivery with Jenkins Pipelines
* [Lab 4](./lab4/): Circuit Breaker Pattern 
* [Lab 5](./lab5/): Envent Streaming  with  Apache Kafka
* [Lab 6](./lab6/): Securing and Testing your Microservices

