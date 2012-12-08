# hadoop-cluster

Set up and run jobs on a cluster.  The cluster and jobs are described as data
maps.

## Usage

The `bin/hadoop` script is a command line interface to build clusters and run
jobs.

You will need a `cluster_spec.clj` to describe your cluster.

```clj
(def java-opts {:jmx-authenticate false :jmx-ssl false})

{:cluster-prefix "hc1"
 :groups {:nn {:node-spec {}
               :count 1
               :roles #{:namenode :jobtracker}}
          :slave {:node-spec {}
                  :count 1
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

Your job will need to be descibed in a `job_spec.clj` file.

```clj
(defn s3n [path]
  (let [aws-key (System/getenv "AWS_KEY")
        aws-secret (System/getenv "AWS_SECRET")]
    (format "s3n://%s:%s@%s" aws-key aws-secret path)))

{:steps
 [{:jar {:remote-file "hadoop-examples-0.20.2-cdh3u0.jar"}
   :main "wordcount"
   :input (s3n "hadoopbook/ncdc/all")
   :output (s3n "your-bucket/hadoop-test")}]
 :on-completion :terminate-cluster}
 ```

You can use different names for these spec files an pass them to `bin/hadoop`
with the relevant switches.

The `bin/hadoop` command supports three commands, `start` will start a cluster,
`job` will run the job_spec on the cluster, and `destroy` will remove the
cluster.

## License

Copyright © 2012 Hugo Duncan

All rights reserved.
