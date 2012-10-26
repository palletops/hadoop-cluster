(ns palletops.cluster.hadoop
  "Builds a hadoop cluster."
  (:use
   [pallet.actions :only [package-manager]]
   [pallet.api :only [group-spec node-spec plan-fn server-spec]]
   [pallet.crate
    :only [def-plan-fn get-settings get-node-settings nodes-with-role]]
   [pallet.crate.automated-admin-user :only [automated-admin-user]]
   [pallet.crate.collectd
    :only [collectd-config collectd-conf collectd-settings collectd-user
           collectd-init-service install-collectd]]
   [pallet.crate.etc-hosts :only [set-hostname]]
   [pallet.crate.graphite :only [graphite]]
   [palletops.crate.hadoop :only [data-node name-node job-tracker task-tracker]]
   [pallet.crate.java :only [install-java java-settings]]
   [pallet.node :only [primary-ip]]))

(def-plan-fn collectd-settings-map
  []
  [[logger] (nodes-with-role :graphite)
   {:keys [carbon-config]} (get-node-settings (:node logger) :graphite {})]
  (m-result
   {:install-strategy :collectd5-ppa
    :config (collectd-config
             (Plugin syslog
                     LogLevel 'info)
             (Plugin cpu)
             (Plugin interface)
             (Plugin load)
             (Plugin memory)
             (Plugin write_graphite
                     (Carbon
                      Host (primary-ip (:node logger))
                      Port (str (or
                                 (-> carbon-config :cache :LINE_RECEIVER_PORT)
                                 2003))
                      ;; EscapeCharacter "_"
                      StoreRates "false"
                      Prefix "collectd."
                      AlwaysAppendDS "false")))}))

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
            :configure (plan-fn (collectd-conf {})
                                (collectd-init-service {:action :start}))
            :restart-collectd (plan-fn
                               (collectd-init-service {:action :restart}))}))

(def graphite-server
  (server-spec
   :extends [(graphite graphite-settings)]
   :roles #{:graphite}))

;;; # Group Specs

(def logging-group
  (group-spec
   :graphite
   :extends [base-server graphite-server]))

(def collectd-group
  (group-spec
   :collectd
   :extends [base-server (collectd-server)]))

(def java
  (server-spec
   :phases {:settings (java-settings {:vendor :openjdk})
            :install (install-java)}))

(def namenode-settings {})

(def namenode-group
  (group-spec
   "namenode"
   :extends [base-server java
             (name-node namenode-settings)
             (job-tracker namenode-settings)
             (collectd-server)]))

(def datanode-settings {})

(def datanode-group
  (group-spec
   "datanode"
   :extends [base-server java
             (data-node datanode-settings)
             (task-tracker datanode-settings)
             (collectd-server)]))
