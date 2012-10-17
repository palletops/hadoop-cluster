(ns palletops.cluster.hadoop
  "Builds a hadoop cluster"
  (:use
   [pallet.actions :only [package-manager]]
   [pallet.api :only [group-spec plan-fn server-spec]]
   [pallet.crate
    :only [def-plan-fn get-settings get-node-settings nodes-with-role]]
   [pallet.crate.automated-admin-user :only [automated-admin-user]]
   [pallet.crate.collectd
    :only [collectd-config collectd-conf collectd-settings collectd-user
           collectd-init-service install-collectd]]
   [pallet.crate.etc-hosts :only [set-hostname]]
   [pallet.crate.graphite :only [graphite]]
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
                      Port (str (-> carbon-config :cache :LINE_RECEIVER_PORT))
                      ;; EscapeCharacter "_"
                      StoreRates "false"
                      Prefix "carbon.agents."
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
                      (collectd-user {})
                      (install-collectd))
            :configure (plan-fn (collectd-conf {})
                                (set-hostname)
                                (collectd-init-service {:action :start}))}))

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


;; (deftest ^:live-test live-test
;;   (let [settings {}]
;;     (doseq [image (images)]
;;       (test-nodes
;;        [compute node-map node-types [; :install
;;                                      :collect-ssh-keys
;;                                      :configure
;;                                      :install-test
;;                                      :configure-test]]
;;        {:namenode
;;         (group-spec
;;          "namenode"
;;          :image image
;;          :count 1
;;          :extends [java (name-node settings) (job-tracker settings)]
;;          :phases {:bootstrap (plan-fn (automated-admin-user))
;;                   :install-test (plan-fn (download-books))
;;                   :configure-test (plan-fn (import-books-to-hdfs))
;;                   :run-test (plan-fn (run-books))
;;                   :post-run (plan-fn (get-books-output))})
;;          :datanode
;;          (group-spec
;;           "datanode"
;;           :image image
;;           :count 1
;;           :extends [java (data-node settings) (task-tracker settings)]
;;           :phases {:bootstrap (plan-fn (automated-admin-user))})}
;;        (let [op (lift (:namenode node-types)
;;                       :phase [:run-test :post-run]
;;                       :compute compute)]
;;          @op
;;          (is (complete? op)))))))
