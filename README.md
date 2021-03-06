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

The following example of cluster spec describes a cluster with one
master and two slave nodes, both using `m1.medium` instances running
Ubuntu 12.04 and a Cloudera Distribution of Hadoop.

```clj
{:cluster-prefix "hc1"
 :groups {:master {:node-spec
                    {:hardware
                      {:hardware-id "m1.medium"}}
                     :count 1
                     :roles #{:namenode :jobtracker}}
          :slave {:node-spec
                   {:hardware
                     {:hardware-id "m1.medium"}}
                  :count 2
                  :roles #{:datanode :tasktracker}}}
 :node-spec {:image
             {:os-family :ubuntu
              :os-version-matches "12.04"
              :os-64-bit true}}
 :hadoop-settings {:dist :cloudera}}
```

Similarly, the job to be run needs to be described. Below is an
example of a job spec that runs the Hadoop word count.

```clj
{:steps
 [{:jar {:remote-file "hadoop-examples-0.20.2-cdh3u0.jar"}
   :main "wordcount"
   :input "s3n://pallet-play/hadoop-examples"
   :output "s3n//<your-dest-bucket>/<your-dest-directory>"}]
 :on-completion :terminate-cluster}
```

The jar file to be run can be specified in several ways. A file already on the
jobtracker can be specified with `:remote-file`.  A local file can be specified
with `:local-file`.  A file at a given url can be specified with `:url`.

## Usage

Checkout this project.

Edit the file `credentials.clj` with your AWS identity and key.

Open the file `cluster_spec.clj` and decide if you want to change the
nodes hardware id or memory, and the number of slave. This file should
work fine without any changes.

Start the cluster:

```bash
$ lein with-profiles +jclouds run  start
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
$ lein with-profiles +jclouds run job job_spec.clj
```

The logging should give you an indication of the job progression. If
the job finalizes correctly, the results will be found in the bucket
and directory specified in the job spec and the cluster will be
destroyed automatically.

If the job fails, the cluster will not be destroyed, but you can
manually destroy the cluster by running:

```bash
$ lein with-profiles +jclouds run destroy
```

## License

Licensed under [EPL](http://www.eclipse.org/legal/epl-v10.html)

All rights reserved.
