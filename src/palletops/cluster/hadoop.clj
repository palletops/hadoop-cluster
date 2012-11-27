(ns palletops.cluster.hadoop
  "Builds a hadoop cluster."
  (:use
   [clojure.string :only [join]]
   [clojure.tools.logging :only [debugf]]
   [pallet.actions :only [package-manager]]
   [pallet.api :only [cluster-spec group-spec node-spec plan-fn server-spec]]
   [pallet.crate
    :only [def-plan-fn get-settings get-node-settings nodes-with-role
           target-name]]
   [pallet.crate.automated-admin-user :only [automated-admin-user]]
   [pallet.crate.collectd
    :only [collectd-config collectd-conf collectd-settings collectd-user
           collectd-service collectd-service-script install-collectd
           collectd-add-plugin-config collectd-plugin-config jmx-mbeans
           mbean mbean-value mbean-table]]
   [pallet.crate.etc-hosts :only [set-hostname]]
   [pallet.crate.graphite :only [graphite]]
   [palletops.crate.hadoop :only [hadoop-server-spec]]
   [palletops.hadoop-config :only [default-node-config]]
   [pallet.crate.java :only [install-java java-settings]]
   [pallet.node :only [hostname primary-ip]]
   [pallet.utils :only [apply-map]]))

(def-plan-fn collectd-settings-map
  []
  [[logger] (nodes-with-role :graphite)
   {:keys [carbon-config]} (get-node-settings (:node logger) :graphite {})
   hostname target-name]
  (m-result
   {:install-strategy :pallet.crate.collectd/source ; :collectd5-ppa
    :features [:java]
    :config `[[:Hostname ~hostname]
              [:Plugin :logfile
               [[:LogLevel ~'info]
                [:File "/var/log/collectd.log"]]]
              [:Plugin ~'syslog [[:LogLevel ~'info]]]
              [:Plugin ~'cpu []]
              [:Plugin ~'interface []]
              [:Plugin ~'load []]
              [:Plugin ~'memory []]
              [:Plugin ~'swap
               [[:ReportByDevice false]
                [:ReportBytes false]]]
              [:Plugin ~'write_graphite
               [[:Carbon
                 [[:Host ~(primary-ip (:node logger))]
                  [:Port ~(str (or
                                (-> carbon-config :cache :LINE_RECEIVER_PORT)
                                2003))]
                  ;; EscapeCharacter "_"
                  [:StoreRates false]
                  [:Prefix "collectd."]
                  [:AlwaysAppendDS false]]]]]]}))

(def graphite-settings
  {:webapp-bind-address "0.0.0.0:8080"})


;;; # Server Specs
(def base-server
  (server-spec
   :phases {:bootstrap (plan-fn
                        (package-manager :update)
                        (automated-admin-user)
                        (set-hostname))}))

(defn collectd-server
  "Basic collectd server-spec"
  []
  (server-spec
   :phases {:settings
            (plan-fn
             [settings (collectd-settings-map)]
             (collectd-settings settings))
            :install (plan-fn
                      (set-hostname)
                      (collectd-user {})
                      (install-collectd))
            :configure (plan-fn
                        (collectd-service-script {})
                        (collectd-conf {})
                        (collectd-service
                         {:action :restart :if-config-changed true}))
            :restart-collectd (plan-fn
                               (collectd-service {:action :restart}))}))

(def graphite-server
  (server-spec
   :extends [(graphite graphite-settings)]
   :roles #{:graphite}))


(def java
  (server-spec
   :phases {:settings (java-settings {:vendor :openjdk})
            :install (install-java)}))

;;; # Group Specs

(def graphite-group
  (group-spec
   :graphite
   :extends [base-server graphite-server (collectd-server)]))

(def collectd-group
  (group-spec
   :collectd
   :extends [base-server (collectd-server)]))

(def namenode-settings {})

(def namenode-group
  (group-spec
   "namenode"
   :extends [base-server java
             (hadoop-server-spec :namenode namenode-settings)
             (hadoop-server-spec :jobtracker namenode-settings)
             (collectd-server)]))

(def datanode-settings {})

(def datanode-group
  (group-spec
   "datanode"
   :extends [base-server java
             (hadoop-server-spec :datanode datanode-settings)
             (hadoop-server-spec :tasktracker datanode-settings)
             (collectd-server)]))

;;; # Data based configuration

(def hadoop-java-env-var
  {:namenode :HADOOP_NAMENODE_OPTS
   :secondary-namenode :HADOOP_SECONDARYNAMENODE_OPTS
   :jobtracker :HADOOP_JOBTRACKER_OPTS
   :datanode :HADOOP_DATANODE_OPTS
   :tasktracker :HADOOP_TASKTRACKER_OPTS})

(def default-cluster-config
  {:namenode {:java {:jmx-port 3000 :jmx-authenticate false}}
   :secondary-namenode {:java {:jmx-port 3001 :jmx-authenticate false}}
   :jobtracker {:java {:jmx-port 3002 :jmx-authenticate false}}
   :datanode {:java {:jmx-port 3003 :jmx-authenticate false}}
   :tasktracker {:java {:jmx-port 3004 :jmx-authenticate false}}})


(def java-system-property
  {:jmx-authenticate "com.sun.management.jmxremote.authenticate"
   :jmx-password-file "com.sun.management.jmxremote.password.file"
   :jmx-port "com.sun.management.jmxremote.port"
   :jmx "com.sun.management.jmxremote"
   :jmx-ssl "com.sun.management.jmxremote.ssl"
   :jmx-ssl-registry "com.sun.management.jmxremote.registry.ssl"
   :jmx-ssl-client-auth "com.sun.management.jmxremote.ssl.need.client.auth"
   :key-store "javax.net.ssl.keyStore"
   :key-store-type "javax.net.ssl.keyStoreType"
   :key-store-password "javax.net.ssl.keyStorePassword"
   :trust-store "javax.net.ssl.trustStore"
   :trust-store-type "javax.net.ssl.trustStoreType"
   :trust-store-password "javax.net.ssl.trustStorePassword"})

(defn java-system-properties
  "Return a java argument string to set the system properties specified in
   `options`."
  [options]
  (join " " (map
             #(format "-D%s%s%s"
                      (java-system-property (key %))
                      (if (nil? (val %)) "" "=")
                      (if (nil? (val %)) "" (val %)))
             options)))

(defn jmx-options
  "Enable port based JMX, with no security.
   Pass a truthy value to `:jmx-local` to enable local JMX access.
   Pass :jmx-password-file to set the password file."
  [{:keys [jmx-port jmx-local jmx-password-file
           jmx-ssl jmx-ssl-client-auth jmx-ssl-registry
           jmx-authenticate]
    :as options}]
  (let [options (if jmx-local
                  (-> options (dissoc :jmx-local) (assoc :jmx-port nil))
                  options)]
    (java-system-properties options)))

(defn ssl-options
  "Return system properties string to control use of ssl."
  [{:keys [key-store key-store-type key-store-password
           trust-store trust-store-type trust-store-password]
    :as options}]
  (java-system-properties options))

(defn hadoop-mbeans
  "Return the collectd spec for specified beans, named with a given `prefix`.
   Known bean components are :namenode-state :namenode-activity,
   :namenode-info :datanode-activity :datanode-info :datanode-state
   :jobtracker-info :tasktracker-info :rpc-act and :rpc-act-detail."
  [prefix components]
  (map
   {:namenode-state
    (mbean
     (str prefix "-nn-state") "hadoop:service=NameNode,name=FSNamesystemState"
     {:prefix (str prefix ".nn-state")}
     (mbean-value "OpenFileDescriptorCount" "gauge" :prefix "filedes.open"))

    :namenode-activity
    (mbean
     (str prefix "-nn-act") "hadoop:service=NameNode,name=NameNodeActivity"
     {:prefix (str prefix ".nn-act")}
     (mbean-value "AddBlockOps" "gauge" :prefix "add.block-ops")
     (mbean-value "fsImageLoadTime" "gauge" :prefix "fs-image-load-time")
     (mbean-value "FilesRenamed" "gauge" :prefix "files.renamed")
     (mbean-value "SyncsNumOps" "gauge" :prefix "syncs.num-ops")
     (mbean-value "SyncsAvgTime" "gauge" :prefix "syncs.avg-time")
     (mbean-value "SyncsMinTime" "gauge" :prefix "syncs.min-time")
     (mbean-value "SyncsMaxTime" "gauge" :prefix "syncs.max-time"))

    :namenode-info
    (mbean
     (str prefix "-nn-info") "hadoop:service=NameNode,name=NameNodeInfo"
     {:prefix (str prefix ".nn-info") :from "prefix"}
     )

    :datanode-activity
    (mbean
     (str prefix "-dn-act") "hadoop:service=DataNode,name=DataNodeActivity-*"
     {:prefix (str prefix ".dn-act") :from "prefix"}
     (mbean-value "bytes_read" "gauge" :prefix "bytes.read")
     (mbean-value "bytes_written" "gauge" :prefix "bytes.written")
     (mbean-value "blocks_read" "gauge" :prefix "blocks.read")
     (mbean-value "blocks_written" "gauge" :prefix "blocks.written")
     (mbean-value "blocks_replicated" "gauge" :prefix "blocks.replicated")
     (mbean-value "blocks_removed" "gauge" :prefix "blocks.removed")
     (mbean-value "blocks_verified" "gauge" :prefix "blocks.verified")
     (mbean-value "writes_from_local_client" "gauge"
                  :prefix "local-client-writes")
     (mbean-value "writes_from_remote_client" "gauge"
                  :prefix "remote-client-writes")
     (mbean-value "readBlockOpAvgTime" "gauge"
                  :prefix "read-block.avg-time")
     (mbean-value "readBlockOpMinTime" "gauge"
                  :prefix "read-block.min-time")
     (mbean-value "readBlockOpMaxTime" "gauge"
                  :prefix "read-block.max-time")
     (mbean-value "readBlockOpNumOps" "gauge" :prefix "read-block.num-ops"))

    :datanode-info
    (mbean
     (str prefix "-dn-info") "hadoop:service=DataNode,name=DataNodeInfo"
     {:prefix (str prefix ".dn-info")}
     )

    :datanode-state
    (mbean
     (str prefix "-dn-state") "hadoop:service=DataNode,name=FSDatasetState-*"
     {:prefix (str prefix ".runtime")}
     )

    :jobtracker-info
    (mbean
     (str prefix "-jt-info") "hadoop:service=JobTracker,name=JobTrackerInfo"
     {:prefix (str prefix ".jt-info")}
     )

    :tasktracker-info
    (mbean
     (str prefix "-tt-info") "hadoop:service=TaskTracker,name=TaskTrackerInfo"
     {:prefix (str prefix ".tt-info")})

    :rpc-act
    (mbean
     (str prefix "-rpc-act") "hadoop:service=*,name=RpcActivityFor*"
     {:prefix (str prefix ".rpc-act")}
     (mbean-value "CallQueueLen" "gauge"))

    :rpc-act-detail
    (mbean
     (str prefix "-rpc-act-detail")
     "hadoop:service=*,name=RpcDetailedActivityFor*"
     {:prefix (str prefix ".rpc-act-detail")})}
   components))

(def jmx-endpoint "service:jmx:rmi:///jndi/rmi://localhost:%s/jmxrmi")

(defn collectd-server-spec
  "Create a server spec that will log hadoop daemons via JMX"
  [settings-fn roles]
  (let [mbeans (concat
                (jmx-mbeans
                 "jvm"
                 [:os :memory :memory-pool :gc :runtime :threading
                  :compilation :class-loading])
                (hadoop-mbeans
                 "hadoop"
                 [;; :namenode-state
                  :namenode-activity
                  ;; :namenode-info
                  :datanode-activity
                  ;; :datanode-info :datanode-state
                  ;; :jobtracker-info :tasktracker-info :rpc-act
                  ]))
        connections (fn [settings hostname]
                      (mapcat
                       (fn [role]
                         (when-let [{:keys [java]} (settings role)]
                           (when-let [port (:jmx-port java)]
                             (collectd-plugin-config
                              :generic-jmx-connection
                              {:url (format jmx-endpoint port)
                               :prefix (str (name role) ".")
                               :mbeans mbeans
                               :host hostname}))))
                       roles))]
    (server-spec
     :extends [(collectd-server)]
     :phases {:settings
              (plan-fn
               [hostname target-name
                settings settings-fn]
               (collectd-add-plugin-config
                :java
                [[:JVMArg "-verbose:jni"]
                 [:JVMArg "-Djava.class.path=/opt/collectd/bindings/java"]
                 [:JVMArg "-Djava.library.path=/usr/local/lib/collectd"]
                 (collectd-plugin-config
                  :generic-jmx
                  {:prefix "jvm"
                   :mbeans mbeans
                   :connections (connections settings hostname)})]))})))

(defn hadoop-group-spec
  [base-node-spec settings-fn group]
  (let [{:keys [node-spec count roles]} (val group)]
    (debugf "hadoop-group-spec roles %s" (vec roles))
    (merge
     (merge base-node-spec node-spec)
     (group-spec
      (key group)
      :extends (concat
                [base-server
                 java
                 (collectd-server-spec settings-fn roles)]
                (map #(hadoop-server-spec % settings-fn) roles))
      :count count))))

(defn hadoop-cluster
  "Returns a cluster-spec for a hadoop cluster, configured as per the arguments"
  [prefix {:keys [groups node-spec] :as config}]
  (let [settings (->> (dissoc config :groups :node-spec)
                      (map (fn group-spec [[role {:keys [java] :as args}]]
                             [role
                              (merge
                               args
                               {:env-vars
                                {(hadoop-java-env-var role)
                                 (join " " [(ssl-options java)
                                            (jmx-options java)])}})]))
                      (into {}))
        settings-fn (plan-fn
                     [config (default-node-config (:config settings))]
                     (m-result (debugf "default-node-config %s" config))
                     (m-result (assoc settings :config config)))]
    (cluster-spec
     prefix
     :groups (conj
              (map (partial hadoop-group-spec node-spec settings-fn) groups)
              graphite-group))))


;;; Example
(comment
  (hadoop-cluster
   "hc1"
   {:groups {:nn {:node-spec {}
                  :count 1
                  :roles #{:namenode :jobtracker}}
             :slave {:node-spec {}
                     :count 1
                     :roles #{:datanode :tasktracker}}}
    :namenode {:java {:jmx-port 3000}}
    :secondary-namenode {:java {:jmx-port 3001}}
    :jobtracker {:java {:jmx-port 3002}}
    :datanode {:java {:jmx-port 3003}}
    :tasktracker {:java {:jmx-port 3004}}}))
