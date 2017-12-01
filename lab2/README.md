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

#### Creating application templates <a name="createtpl"></a>
#### Parameterizing templates <a name="parametertpl"></a>
#### Using templates <a name="usetpl"></a>

###  Next Steps <a name="introduction"></a>

* [Lab 3](../lab3/): CI/CD with Jenkins2  Pipenlines

