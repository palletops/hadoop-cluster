# hadoop-cluster

Set up and run jobs on a cluster.  The cluster and jobs are described as data
maps.

## Usage

The `bin/hadoop` script is a command line interface to build clusters and run
jobs.  The script supports three commands; `start` will start a cluster, `job`
will run the job_spec on the cluster, and `destroy` will remove the cluster.

You will need a file to describe your cluster. By default `cluster_spec.clj` is
read from the current directory, and you can specify any file using the
`--spec-file` command line switch.

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

Your cloud credentials should be specified in `credentials.clj`, or in a file
passed with the `--credentials` flag.

As an alternative the credentials can be specified via pallet's
`~/.pallet/config.clj` file.  In this case, pass the name of the required
service with `--profile`.

To start the cluster:

    bin/hadoop start


Your job will need to be descibed in a configuration file. A `job_spec.clj` file
is included in the disribution to get you started.  This configuration file
needs to be passed as an argument to the start comand.

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

The credentials referred to (via environment variables) in the job configuration
above are distinct from the credentials that the CLI uses to create and control
your hadoop cluster.

To run the job:

    bin/hadoop job job_spec.clj

To manually destroy the cluster:

    bin/hadoop destroy

## License

Copyright © 2012 Hugo Duncan

All rights reserved.
