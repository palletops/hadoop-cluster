(ns palletops.cluster.hadoop
  "Builds a hadoop cluster."
  (:require
   [clojure.string :refer [join]]
   [clojure.tools.logging :refer [debugf tracef]]
   [pallet.actions :refer [package-manager plan-when]]
   [pallet.api :refer [cluster-spec group-spec node-spec plan-fn server-spec]]
   [pallet.crate
    :refer [defplan get-settings get-node-settings nodes-with-role
           target-name]]
   [pallet.crate.automated-admin-user :refer [automated-admin-user]]
   [pallet.crate.collectd :as collectd]
   [pallet.crate.etc-hosts :refer [set-hostname]]
   [pallet.crate.graphite :as graphite]
   [palletops.crate.hadoop
    :refer [hadoop-role-ports hadoop-server-spec use-hosts-file]]
   [palletops.hadoop-config
    :refer [default-node-config nested-maps->dotted-keys]]
   [palletops.locos :refer [deep-merge]]
   [pallet.crate.java :as java]
   [pallet.node :refer [hostname primary-ip]]
   [pallet.utils :refer [apply-map]]))

(defplan collectd-settings-map
  []
  (let [[logger] (nodes-with-role :graphite)
        {:keys [carbon-config]} (get-node-settings (:node logger) :graphite {})
        hostname target-name]
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
                        (when (use-hosts-file)
                          (set-hostname)))}))

(defn collectd-server
  "Basic collectd server-spec"
  []
  (server-spec
   :phases {:settings
            (plan-fn
              (let [settings (collectd/settings-map)]
                (collectd/settings settings)))
            :install (plan-fn
                       (when (use-hosts-file)
                         (set-hostname))
                       (collectd/user {})
                       (collectd/install))
            :configure (plan-fn
                         (collectd/service-script {})
                         (collectd/configure {})
                         (collectd/service
                          {:action :restart :if-config-changed true}))
            :restart-collectd (plan-fn
                                (collectd/service {:action :restart}))}))

(def graphite-server
  (server-spec
   :extends [(graphite/server-spec graphite-settings)]
   :roles #{:graphite}))


(def java
  (server-spec
   :phases {:settings (plan-fn (java/settings {:vendor :openjdk}))
            :install (plan-fn (java/install {}))}))

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
(def default-cluster-settings
  {:namenode {:jmx-port 3000 :jmx-authenticate false}
   :secondary-namenode {:jmx-port 3001 :jmx-authenticate false}
   :jobtracker {:jmx-port 3002 :jmx-authenticate false}
   :datanode {:jmx-port 3003 :jmx-authenticate false}
   :tasktracker {:jmx-port 3004 :jmx-authenticate false}})

(defn hadoop-mbeans
  "Return the collectd spec for specified beans, named with a given `prefix`.
   Known bean components are :namenode-state :namenode-activity,
   :namenode-info :datanode-activity :datanode-info :datanode-state
   :jobtracker-info :tasktracker-info :rpc-act and :rpc-act-detail."
  [prefix components]
  (map
   {:namenode-state
    (collectd/mbean
     (str prefix "-nn-state") "hadoop:service=NameNode,name=FSNamesystemState"
     {:prefix (str prefix ".nn-state")}
     (collectd/mbean-value "OpenFileDescriptorCount" "gauge" :prefix "filedes.open"))

    :namenode-activity
    (collectd/mbean
     (str prefix "-nn-act") "hadoop:service=NameNode,name=NameNodeActivity"
     {:prefix (str prefix ".nn-act")}
     (collectd/mbean-value "AddBlockOps" "gauge" :prefix "add.block-ops")
     (collectd/mbean-value "fsImageLoadTime" "gauge" :prefix "fs-image-load-time")
     (collectd/mbean-value "FilesRenamed" "gauge" :prefix "files.renamed")
     (collectd/mbean-value "SyncsNumOps" "gauge" :prefix "syncs.num-ops")
     (collectd/mbean-value "SyncsAvgTime" "gauge" :prefix "syncs.avg-time")
     (collectd/mbean-value "SyncsMinTime" "gauge" :prefix "syncs.min-time")
     (collectd/mbean-value "SyncsMaxTime" "gauge" :prefix "syncs.max-time"))

    :namenode-info
    (collectd/mbean
     (str prefix "-nn-info") "hadoop:service=NameNode,name=NameNodeInfo"
     {:prefix (str prefix ".nn-info") :from "prefix"}
     )

    :datanode-activity
    (collectd/mbean
     (str prefix "-dn-act") "hadoop:service=DataNode,name=DataNodeActivity-*"
     {:prefix (str prefix ".dn-act") :from "prefix"}
     (collectd/mbean-value "bytes_read" "gauge" :prefix "bytes.read")
     (collectd/mbean-value "bytes_written" "gauge" :prefix "bytes.written")
     (collectd/mbean-value "blocks_read" "gauge" :prefix "blocks.read")
     (collectd/mbean-value "blocks_written" "gauge" :prefix "blocks.written")
     (collectd/mbean-value "blocks_replicated" "gauge" :prefix "blocks.replicated")
     (collectd/mbean-value "blocks_removed" "gauge" :prefix "blocks.removed")
     (collectd/mbean-value "blocks_verified" "gauge" :prefix "blocks.verified")
     (collectd/mbean-value "writes_from_local_client" "gauge"
                  :prefix "local-client-writes")
     (collectd/mbean-value "writes_from_remote_client" "gauge"
                  :prefix "remote-client-writes")
     (collectd/mbean-value "readBlockOpAvgTime" "gauge"
                  :prefix "read-block.avg-time")
     (collectd/mbean-value "readBlockOpMinTime" "gauge"
                  :prefix "read-block.min-time")
     (collectd/mbean-value "readBlockOpMaxTime" "gauge"
                  :prefix "read-block.max-time")
     (collectd/mbean-value "readBlockOpNumOps" "gauge" :prefix "read-block.num-ops"))

    :datanode-info
    (collectd/mbean
     (str prefix "-dn-info") "hadoop:service=DataNode,name=DataNodeInfo"
     {:prefix (str prefix ".dn-info")}
     )

    :datanode-state
    (collectd/mbean
     (str prefix "-dn-state") "hadoop:service=DataNode,name=FSDatasetState-*"
     {:prefix (str prefix ".runtime")}
     )

    :jobtracker-info
    (collectd/mbean
     (str prefix "-jt-info") "hadoop:service=JobTracker,name=JobTrackerInfo"
     {:prefix (str prefix ".jt-info")}
     )

    :tasktracker-info
    (collectd/mbean
     (str prefix "-tt-info") "hadoop:service=TaskTracker,name=TaskTrackerInfo"
     {:prefix (str prefix ".tt-info")})

    :rpc-act
    (collectd/mbean
     (str prefix "-rpc-act") "hadoop:service=*,name=RpcActivityFor*"
     {:prefix (str prefix ".rpc-act")}
     (collectd/mbean-value "CallQueueLen" "gauge"))

    :rpc-act-detail
    (collectd/mbean
     (str prefix "-rpc-act-detail")
     "hadoop:service=*,name=RpcDetailedActivityFor*"
     {:prefix (str prefix ".rpc-act-detail")})}
   components))

(def jmx-endpoint "service:jmx:rmi:///jndi/rmi://localhost:%s/jmxrmi")

(defn collectd-server-spec
  "Create a server spec that will log hadoop daemons via JMX"
  [settings-fn roles]
  (let [mbeans (concat
                (collectd/jmx-mbeans
                 "jvm"
                 [:os :memory :memory-pool :gc :runtime :threading
                  :compilation :class-loading])
                (hadoop-mbeans
                 "hadoop"
                 [ ;; :namenode-state
                  :namenode-activity
                  ;; :namenode-info
                  :datanode-activity
                  ;; :datanode-info :datanode-state
                  ;; :jobtracker-info :tasktracker-info :rpc-act
                  ]))
        connections (fn [settings hostname]
                      (mapcat
                       (fn [role]
                         (when-let [config (-> settings :config role)]
                           (when-let [port (:jmx-port config)]
                             (collectd/plugin-config
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
                (let [hostname target-name
                      settings settings-fn]
                  (collectd/add-plugin-config
                   :java
                   [[:JVMArg "-verbose:jni"]
                    [:JVMArg "-Djava.class.path=/opt/collectd/bindings/java"]
                    [:JVMArg "-Djava.library.path=/usr/local/lib/collectd"]
                    (collectd/plugin-config
                     :generic-jmx
                     {:prefix "jvm"
                      :mbeans mbeans
                      :connections (connections settings hostname)})])))})))

(defmethod hadoop-server-spec :graphite
  [_ settings-fn & {:keys [instance-id] :as opts}]
  graphite-server)

(defn port-spec [roles]
  {:network {:inbound-ports
             (distinct
              (conj
               (->> (map hadoop-role-ports roles)
                    (mapcat
                     (fn [role-map]
                       (mapcat (or role-map {}) [:internal :external]))))
               22))}})

(defn hadoop-group-spec
  [base-node-spec settings-fn features group]
  (let [{:keys [node-spec count roles extends]} (val group)]
    (debugf "hadoop-group-spec roles %s" (vec roles))
    (group-spec
     (key group)
     :extends (concat
               [base-server java]
               (if (features :collectd)
                 [(collectd-server-spec settings-fn roles)])
               (map #(hadoop-server-spec % settings-fn) roles)
               extends)
     :count count
     :node-spec (deep-merge (port-spec roles) base-node-spec node-spec))))

(defn hadoop-cluster
  "Returns a cluster-spec for a hadoop cluster, configured as per the arguments.

  `:hadoop-config` a map of hadoop configuration properties
  `:settings`      a map of settings passed to each role-spec

  You can also specify configuration for each role, as a map under the name of
  role as a keyword.

  (hadoop-cluster
   \"hc1\"
   {:groups {:nn {:node-spec {}
                  :count 1
                  :roles #{:namenode :jobtracker}}
             :slave {:node-spec {}
                     :count 1
                     :roles #{:datanode :tasktracker}}}
    :hadoop-config
     {:io.file.buffer.size 65536
      :config {
       :namenode {:jmx-port 3000}
       :secondary-namenode {:jmx-port 3001}
       :jobtracker {:jmx-port 3002}
       :datanode {:jmx-port 3003}
       :tasktracker {:jmx-port 3004}}}})"
  [{:keys [groups config hadoop-settings node-spec cluster-prefix]
    :or {cluster-prefix "hc"}
    :as settings}]
  (let [hadoop-settings (deep-merge
                         default-cluster-settings
                         (nested-maps->dotted-keys config "pallet.")
                         hadoop-settings)
        _ (tracef "hadoop-cluster hadoop-settings %s"
                  hadoop-settings) ; may log credentials!
        settings-fn (plan-fn (default-node-config hadoop-settings))
        roles (into #{} (mapcat :roles (vals groups)))
        features (set (when (roles :graphite) [:collectd]))]
    (cluster-spec
     cluster-prefix
     :groups (map
              (partial hadoop-group-spec node-spec settings-fn features)
              groups))))
