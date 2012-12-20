# hadoop-cluster

Set up and run jobs on a cluster.  The cluster and jobs are described as data
maps.

## Introduction

The `bin/hadoop` script is a command line interface to build clusters and run
jobs.  The script supports three commands; `start` will start a cluster, `job`
will run the job_spec on the cluster, and `destroy` will force the
removal of the cluster.

To launch a new Hadoop cluster you will first need a description of the cluster
to build. This is done by creating a cluster spec file.

The following example of cluster spec describes a cluster with one master and
two slave nodes, both using `m1.large` instances (7.5GB of RAM) running Ubuntu
12.04 and a Cloudera Distribution of Hadoop.

```clj
(def java-opts {:jmx-authenticate false :jmx-ssl false})

{:cluster-prefix "hc1"
 :groups {:master {:node-spec 
                 {:hardware
                   {:hardware-id "m1.large"
                    :min-ram 2048}}
               :count 1
               :roles #{:namenode :jobtracker}}
          :slave {:node-spec 
                    {:hardware
                      {:hardware-id "m1.large"
                       :min-ram 4096}}
                  :count 2
                  :roles #{:datanode :tasktracker}}}
 :node-spec {:image
             {:os-family :ubuntu :os-version-matches "12.04" :os-64-bit true}}
 :hadoop-settings
 {:dist :cloudera
  :config
  {:namenode (merge {:jmx-port 3000} java-opts)
   :secondary-namenode (merge {:jmx-port 3001} java-opts)
   :jobtracker (merge {:jmx-port 3002} java-opts)
   :datanode (merge {:jmx-port 3003} java-opts)
   :tasktracker (merge {:jmx-port 3004} java-opts)}}}
```

Similarly, the job to be run needs to be described. Below is an
example of a job spec that runs the Hadoop word count. 

```clj
(defn s3n [path]
  (let [aws-key <your-aws-s3-key>
        aws-secret <your-aws-s3-secret>]
    (format "s3n://%s:%s@%s" aws-key aws-secret path)))

{:steps
 [{:jar {:remote-file "hadoop-examples-0.20.2-cdh3u0.jar"}
   :main "wordcount"
   :input (s3n "pallet-play/hadoop-examples")
   :output (s3n "<your-dest-bucket>/<your-dest-directory>")}]
 :on-completion :terminate-cluster}
```

## Usage

Download and uncompress `palletops-hadoop.tar.gz`.

```bash
$ tar xzf palletops-hadoop.tar.gz
...
$ cd palletops-hadoop
```

Edit the file `credentials.clj` with your AWS identity and key.

Open the file `cluster_spec.clj` and decide if you want to change the
nodes hardware id or memory, and the number of slave. This file should
work fine without any changes.

Edit the file `job_spec.clj` adding your s3 credentials and the
destination bucket and directory.

Start the cluster:

```bash
$ bin/hadoop start
```

If all works correctly, you should get something like this after a few
log lines:

```
{"107.20.115.138"
 {:roles #{:datanode :namenode :jobtracker},
  :private-ip "10.80.133.215",
  :hostname "hc1-master-c8a4f1b6"},
 "174.129.113.172"
 {:roles #{:datanode :tasktracker},
  :private-ip "10.36.105.170",
  :hostname "hc1-slave-dca4f1a2"},
 "184.72.91.105"
 {:roles #{:datanode :tasktracker},
  :private-ip "10.82.254.132",
  :hostname "hc1-slave-dea4f1a0"}}
```

Now it is time to run the job. At the shell, run:

```bash
$ bin/hadoop job job_spec.clj
```

The logging should give you an indication of the job progression. If
the job finalizes correctly, the results will be found in the bucket
and directory specified in the job spec and the cluster will be
destroyed automatically.

If the job fails, the cluster will not be destroyed, but you can
manually destroy the cluster by running:

```bash
$ bin/hadoop destroy
```

## License

Copyright © 2012 Hugo Duncan and Antoni Batchelli

All rights reserved.
