# LAB 2:  Creating persistent database and reusable application templates

# Table of contents
1. [Introduction](#introduction)
2. [Persisting Application Data](#persistence)
    1. [UAT Personnedb](#pvcdb)
    2. [Persistent Volume Claim](#pvc)
    3. [Volumes](#volume)
3. [Reusable Application templates](#templates)
    1. [Creating templates](#createtpl)
    2. [Parameterizing templates](#parametertpl)
    3. [Using templates](#usetple)
4. [Next Labs](#next)


### Introduction <a name="introduction"></a>
In lab01, you created an ephemeral MongoDB to store Microservices data, You also create various Kubernetes objects 
to build, deploy,expose your application. In the following lab, we will see how to make data persistent in the UAT database,
we will also see how to create custom and reusable application templates to recreate the application quickly in another 
environment than msa-dev

---
### Persisting Applicaiton Data <a name="persistence"></a>
In case the personnedb pod goes down in the msa-dev projet, data inserted in the previous lab are lost.
Try to verify this behaviour by performing the following actions

```
 oc scale dc/personnedb -n msa-dev --replicas=0 
```
This operation terminated the running pod. Try to bring back the database pod with the same command but  ``` --replicas=1```

```
curl -H 'Content-Type: Application/json' http://personneapi-msa-dev.192.168.99.100.nip.io/Personne/
Result => []
```
application data are lost. In the following lines we will show how to persist The personne data on a persistent volume.


#### Setup UAT Personnedb  <a name="pvcdb"></a>

As we are moving to the production stage, with custom data set, we will create a specific and persitent database for the UAT environment.


```
oc new-project msa-uat
```

We now have two projects, msa-dev and *msa-uat*. 
To avoid confusion, we will explicitly specify on which namespace the command applied using the e *oc -n option*

Create the uat database using the following command.

``` 
oc new-app --docker-image=mongo:latest --name=personnedb -n msa-uat
```
As result, an *ephemeral* MongoDB service presonnedb is created and started  in the  msa-uat  namespace.
when browsing the dc/personnedb created in the namespace, it is easier to see that the database make use of two emptydir volumes

```
volumeMounts:
        - mountPath: /data/configdb
          name: personnedb-volume-1
        - mountPath: /data/db
          name: personnedb-volume-2
 ```

**personnedb-volume-1** is used for DB configuraiton while **personnedb-volume-2** is used to store data
To make mongo collections persistent, we need to change the personndb-volume-2 from emptyDir volume to a persistentVolumeClaim volume.



#### Peristent Volume Claim  <a name="pvc"></a>
Persistent Volume claims refers to storage requests 
Let's create a mongo pvc from the following [json template](./mongo-pvc.json) 


```
apiVersion: "v1"
kind: "PersistentVolumeClaim"
metadata:
  name: "mongo-pvc"
spec:
  accessModes:
    - "ReadWriteMany"
  resources:
    requests:
      storage: "1Gi"

```
we request 1Gi volume accessible in Read/WriteManay


Create the pvc object using the command 

```
$ oc create -f mongo-pvc.json
```
If Persistent volume objects exist in your cluster or are dynamcally provision, the newly created pvc is automatically bound to a new Persistent volume 


 ```
$ oc get pvc
NAME        STATUS    VOLUME    CAPACITY   ACCESSMODES   AGE
mongo-pvc   Bound     pv0068    100Gi      RWO,ROX,RWX   1d
```
Here the mongo-pvc pvc is bound to persistent volume *pv0068*. In the next section we will se how to associate this volume to our database and check Personne collection is persisted accross containers restarts.

#### Peristent Volume   <a name="pv"></a>

To make the personne-db-volume-2  point on mongo-pvc persitent volume claim , use the following command
```
oc volume --add=true  --name=personnedb-volume-2 --overwrite  --type=persistentVolumeClaim  --claim-name=mongo-pvc --claim-mode=rwm   dc/personnedb
```
the --overwrite option is mandatory since the volume already exists.
another way to do it is to  remove the personnedb-volume-2 and recreate it as persistent volume claim.

Once the configuraiton is applied, a new pod is started with the dabase.
By default Minishift has 100 pv available with 100Gi.
Let's explore the pv0068 on which the pvc is created.

```
$ oc get pv | grep pv0068
pv0068    100Gi      RWO,ROX,RWX   Recycle         Bound       msa-uat/mongo-pvc             2d


$ oc describe pv pv0068
Name:		pv0068
Labels:		volume=pv0068
StorageClass:
Status:		Bound
Claim:		msa-uat/mongo-pvc
Reclaim Policy:	Recycle
Access Modes:	RWO,ROX,RWX
Capacity:	100Gi
Message:
Source:
    Type:	HostPath (bare host directory volume)
    Path:	/var/lib/minishift/openshift.local.pv/pv0068
No events.

```



```
$ minishift ssh ' ls -rtl /var/lib/minishift/openshift.local.pv/pv0068'
total 192
-rw-r--r--. 1 polkitd ssh_keys    21 Nov 30 05:23 WiredTiger.lock
-rw-r--r--. 1 polkitd ssh_keys    49 Nov 30 05:23 WiredTiger
-rw-r--r--. 1 polkitd ssh_keys  4096 Nov 30 05:23 WiredTigerLAS.wt
drwxr-xr-x. 2 polkitd ssh_keys  4096 Nov 30 05:23 journal
-rw-r--r--. 1 polkitd ssh_keys     2 Nov 30 05:23 mongod.lock
-rw-r--r--. 1 polkitd ssh_keys    95 Nov 30 05:23 storage.bson
-rw-r--r--. 1 polkitd ssh_keys 16384 Nov 30 05:24 index-4--7248922805352944788.wt
-rw-r--r--. 1 polkitd ssh_keys 16384 Nov 30 05:24 sizeStorer.wt
-rw-r--r--. 1 polkitd ssh_keys 16384 Nov 30 05:24 index-3--7248922805352944788.wt
-rw-r--r--. 1 polkitd ssh_keys 16384 Nov 30 05:24 index-1--7248922805352944788.wt
-rw-r--r--. 1 polkitd ssh_keys 16384 Nov 30 05:24 collection-2--7248922805352944788.wt
-rw-r--r--. 1 polkitd ssh_keys 16384 Nov 30 05:24 collection-0--7248922805352944788.wt
-rw-r--r--. 1 polkitd ssh_keys 16384 Nov 30 05:24 _mdb_catalog.wt
-rw-r--r--. 1 polkitd ssh_keys 49152 Nov 30 05:25 WiredTiger.wt
-rw-r--r--. 1 polkitd ssh_keys   994 Nov 30 05:25 WiredTiger.turtle
drwxr-xr-x. 2 polkitd ssh_keys  4096 Nov 30 06:46 diagnostic.data
```

Let's connect to the personnedb current pod, insert a user in mongo collection and kill the pod

```
$oc get pods
NAME                 READY     STATUS    RESTARTS   AGE
personnedb-3-lwq2k   1/1       Running   0          1d

$ oc rsh  personnedb-3-lwq2k
# mongo
MongoDB shell version v3.4.10
connecting to: mongodb://127.0.0.1:27017
MongoDB server version: 3.4.10
Server has startup warnings:
....
> db.personne.insertOne({ "_id" : "001","firstName" : "Nono", "lastName" : "Elvadas" })
{ "acknowledged" : true, "insertedId" : "001" }
> exit
bye

#exit

$oc delete pod personnedb-3-lwq2k
pod "personnedb-3-lwq2k" deleted

```

As replicas is set to 1, Openshift replication controller  will create a new pod for the personnedb mongo database.

```
$ oc get pods
NAME                 READY     STATUS    RESTARTS   AGE
personnedb-3-kl5fr   1/1       Running   0          1d


$ oc rsh  personnedb-3-lwq2k
# mongo

> db.personne.find({ "_id" : "001"})
{ "_id" : "001", "firstName" : "Nono", "lastName" : "Elvadas" }
>
```

Personne items now stay alive even if the databae pod are restarted.
In this section, you learn how to create a Persistence volume claim to persist application data.
In the next section, we will see how to create, customize and use  application template in order to accelerate application provisionning and delivery in a multitenant cluster.


### Reusable Application templates <a name="templates"></a>
An application template is a collection of Kubernetes objects gather togeher in a JSON or Yaml file.
There a various techniques to create an application template.

#### Creating application templates <a name="createtpl"></a>
The most simple technique to create an application template is to export existing objects of a namespace .
A part from that user can create their own template by extending/modifying exisint templates or from scratch.
In the following section we are going to create a simple template to recreate the personneapi application in another namespace.
Todo so we can export the following objects: dc/personneapi svc/personneapi route/personneapi with the following instruction

```
oc project msa-dev
oc export dc/personneapi svc/personneapi route/personneapi  --as-template=personneapi-template -o json  > personneapi-template-v1.0.json
```
This exports objects as template , then format output in json  and redirect it to a json file.
The template contains an array of json object
```
{
  2     "kind": "Template",
  3     "apiVersion": "v1",
  4     "metadata": {
  5         "name": "personneapi-template",
  6         "creationTimestamp": null
  7     },
  8     "objects": [
  9         {
 10             "kind": "DeploymentConfig",
 11             "apiVersion": "v1",
 12             "metadata": {
 13                 "name": "personneapi",
 14                 "
...
           {
144             "kind": "Service",
145             "apiVersion": "v1",
146             "metadata": {
147                 "name": "personneapi",
148                 "creationTimestamp": null,
149                 "labels": {
150                     "app": "personn
 ....
88         {
189             "kind": "Route",
190             "apiVersion": "v1",
191             "metadata": {
192                 "name": "personneapi",
193                 "creationTimestamp": null,
194                 "labels": {
195                     "app": "personneapi"
196                 },
197                 "annotations": {
198                     "openshift.io/host.generated": "true"
199                 }
200             },
201             "spec": {
202                 "host": "personneapi-msa-dev.192.168.99.100.nip.io",
203                 "path": "/",

```

Template's objects are exported with their current configuration in the current namespace, to be able to recreate the same object in another namespace, 
we neet to apply some customization to the template.


#### Parameterizing templates <a name="parametertpl"></a>

To make the template usefull, we will add three parameters before the objects section
we also need to update some objects values
* route's host
* image tags
```
    "parameters": [
          {
              "displayName": "Application Name",
              "description": "The name for the application.",
              "name": "APPLICATION_NAME",
              "value": "msa",
              "required": true
          },
          {
              "displayName": "Application Environment ",
              "description": "Environment used to create the namespace . Allowed values are: `dev`, `uat`, `pprod` and `prod`.",
              "name": "APPLICATION_ENV",
              "value": "uat",
              "required": true
          },
 
       {
               "displayName": "Image Tag ",
               "description": "Environment used to create the namespace . following values can be used,`uatReady` `prodReady`",
               "name": "IMAGE_TAG",
               "value": "uatReady",
               "required": false
          }

 ]

```
APPLICATION_NAME and APPLICATION_ENV are used to create a namespace combinaison : example msa-dev
a specific image tag (IMAGE_TAG) should be deployed for each environment. *uatReady* for example is the tag to be 
deployed on  UAT environment.

```
{
 69                         "type": "ImageChange",
 70                         "imageChangeParams": {
 71                             "automatic": true,
 72                             "containerNames": [
 73                                 "personneapi"
 74                             ],
 75                             "from": {
 76                                 "kind": "ImageStreamTag",
 77                                 "namespace": "${APPLICATION_NAME}-${APPLICATION_ENV}",
 78                                 "name": "personneapi:${IMAGE_TAG}"
 79                             }
 80                         }
```

The final template is available at [personneapi-template-v1.1.json](./personneapi-template-v1.1.json)
You can add custom modifications in your templates, you can add more objects, object labels, ...

#### Using templates <a name="usetpl"></a>
In this section we will use the previously created template [personneapi-template-v1.1.json](./personneapi-template-v1.1.json) to 
create the personneapi microservice  application in the UAT env.

```
oc process -f  personneapi-template-v1.1.json -v APPLICATION_NAME=msa -v IMAGE_TAG=uatReady -v APPLICATION_ENV=uat | oc create -n msa-uat  -f -

```
Use the -v option to pass parameter to templates
The deployment config, the service and the route are created, but the personneapi has ZERO pods because the personneapi:uatReady tag is not available in the current namespace.

```
$oc get dc
NAME          REVISION   DESIRED   CURRENT   TRIGGERED BY
personneapi   0          1         0         config,image(personneapi:uatReady)
personnedb    3          1         1         config,image(personnedb:latest)
```

To make the application ready, we will first create the relevant  properties configMap
```
$ cd lab1/msa-personne/
  oc create cm props-volume-cm --from-file=./configMap/uat
  configmap "props-volume-cm" created
```

Then tag the imagestream 
```
$ oc tag msa-dev/personneapi:latest msa-uat/personneapi:uatReady
    Tag personneapi:uatReady set to msa-dev/personneapi@sha256:02a9e15d52fe5eb6bfda086d5834a4203d8264fa686ede0941c3bebe1e313082.
```

As soon as the tag becomes available , a new pesronneapi pod is deployed

```
$ oc get pods -n msa-uat
NAME                  READY     STATUS    RESTARTS   AGE
personneapi-2-6nxph   1/1       Running   0          3d
personnedb-3-kl5fr    1/1       Running   0          4d
```

We can verify the persistent user created at the begining of the lab is still in the database.
```
curl -H 'Content-Type: Application/json' http://personneapi-msa-uat.192.168.99.100.nip.io/Personne/
[{“ref":"001","firstName":"Nono","lastName":"Elvadas","birthDate":null,"customTag":null}]
```

In this section we learn how to create application component using templates.
We created a microservice template from the lab1's personneapi. 
we created custom parameters to enrich the template
an application personneapi was created in UAT environment based on the created template.
We manually tag images from dev to UAT to start the application pod. In the next section we will exploire CI/ CD features and see how to automate applicaiton promotion accross environment using Jenkins pipelines.


###  Next Steps <a name="introduction"></a>

* [Lab 3](../lab3/): CI/CD with Jenkins2  Pipenlines

