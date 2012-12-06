(ns palletops.cluster.hadoop-test
  (:use
   clojure.test
   palletops.cluster.hadoop
   [clojure.tools.logging :only [debugf]]
   [pallet.algo.fsmop :only [complete?]]
   [pallet.api :only [group-spec lift plan-fn server-spec]]
   [pallet.live-test :only [images test-nodes]]
   [pallet.script :only [with-script-context]]
   [pallet.stevedore :only [with-script-language]]
   [palletops.crate.hadoop.base :only [base-settings default-settings dist-rules]]
   [palletops.hadoop.hadoop-book-example
    :only [download-books import-books-to-hdfs run-books get-books-output]]))

(def hadoop-book-phases
  {:install-test (plan-fn (download-books))
   :configure-test (plan-fn (import-books-to-hdfs))
   :run-test (plan-fn (run-books))
   :post-run (plan-fn (get-books-output))})

(def hadoop-book-test
  (server-spec :phases hadoop-book-phases))

#_(deftest ^:live-test live-test
  (let [settings {}]
    (doseq [image (images)]
      (test-nodes
       [compute node-map node-types
        [;:install
         :collect-ssh-keys
         ;; :configure :restart-collectd :run :init
         ;; :install-test :configure-test
         ]]
       {:namenode (assoc
                      (group-spec
                       :namenode
                       :extends [namenode-group hadoop-book-test])
                    :image (assoc image
                             :inbound-ports [22 50030 50070 50075 8020 8021])
                    :count 1)
        :datanode (assoc datanode-group
                    :image (assoc image :inbound-ports [22 50060 50075 50030])
                    :count 1)
        :graphite
        (group-spec
         "graphite"
         :image (assoc image :inbound-ports [22 8080 2003])
         :count 1
         :extends [collectd-group logging-group])}
       (let [op (lift [(map node-types [:namenode :graphite :datanode])]
                      :phase [:run-test :post-run]
                      :compute compute)
             _ @op
             complete (complete? op)]
         (debugf "lift Op %s" @op)
         (is complete))))))

(def java-opts {:jmx-authenticate false :jmx-ssl false})
(def cloudera-cluster
  {:cluster-prefix "hc1"
   :groups {:nn {:node-spec {}
                 :count 1
                 :roles #{:namenode :jobtracker}}
            :slave {:node-spec {}
                    :count 1
                    :roles #{:datanode :tasktracker}}
            ;; :graphite {:count 1 :roles #{:graphite}}
            }
   :hadoop-settings
   {:dist :cloudera
    :config
    {:namenode (merge {:jmx-port 3000} java-opts)
     :secondary-namenode (merge {:jmx-port 3001} java-opts)
     :jobtracker (merge {:jmx-port 3002} java-opts)
     :datanode (merge {:jmx-port 3003} java-opts)
     :tasktracker (merge {:jmx-port 3004} java-opts)}}})

(deftest ^:live-test live-test
  (let [cluster (hadoop-cluster cloudera-cluster)]
    (doseq [image (images)]
      (test-nodes
       [compute node-map node-types
        [
         :install
         :collect-ssh-keys
         :configure
         :restart-collectd
         :run :init
         :install-test
         :configure-test
         ]]
       (update-in
        (into {}
              (map
               (juxt :group-name #(assoc-in % [:image] image))
               (:groups cluster)))
        [:hc1-nn :phases] merge hadoop-book-phases)
       (let [op (lift (vals node-types)
                      :phase [:run-test :post-run]
                      :compute compute)
             _ @op
             complete (complete? op)]
         (debugf "lift Op %s" @op)
         (is complete))))))

(deftest mapr-cluster-settings-test
  (let [c (with-script-language :pallet.stevedore.bash/bash
            (with-script-context [:ubuntu]
              (base-settings {:dist :mapr} (default-settings) @dist-rules)))]
    (is (= "2.1.0" (:mapr-version c)))
    (is (= "/opt/mapr" (:mapr-home c)))))

(def mapr-cluster
  {:cluster-prefix "hc1"
   :groups {:nn {:node-spec {}
                 :count 1
                 :roles #{:mapr/fileserver
                          :jobtracker
                          :mapr/cldb
                          :mapr/zookeeper
                          :mapr/webserver}}
            :slave {:node-spec {}
                    :count 1
                    :roles #{:tasktracker
                             :mapr/fileserver}}}
   :node-spec {:hardware {:min-ram 700}}
   :hadoop-settings
   {:dist :mapr
    :config
    {:mapr/fileserver (merge {:jmx-port 3000} java-opts)
     :mapr/webserver (merge {:jmx-port 3006} java-opts)
     :jobtracker (merge {:jmx-port 3002} java-opts)
     :mapr/cldb (merge {:jmx-port 7222} java-opts)
     :mapr/zookeeper (merge {:jmx-port 3007} java-opts)
     :tasktracker (merge {:jmx-port 3004} java-opts)}
    :metrics {"maprmepredvariant.class"
              "com.mapr.job.mngmnt.hadoop.metrics.MaprRPCContext"
              "maprmepredvariant.period" 10
              "maprmapred.class"
              "com.mapr.job.mngmnt.hadoop.metrics.MaprRPCContextFinal"
              "maprmapred.period" 10}}})

#_(deftest ^:live-test live-test-mapr
   (let [cluster (hadoop-cluster mapr-cluster)]
    (doseq [image (images)]
      (test-nodes
       [compute node-map node-types
        [
         :install
         :collect-ssh-keys
         :configure
         :restart-collectd
         :run
         :init
         :install-test
         :configure-test
         ]]
       (update-in
        (into {}
              (map
               (juxt :group-name #(assoc-in % [:image] image))
               (:groups cluster)))
         [:hc1-nn :phases] merge hadoop-book-phases)
       ;; (let [op (lift (vals node-types)
       ;;                :phase [:run-test :post-run]
       ;;                :compute compute)
       ;;       _ @op
       ;;       complete (complete? op)]
       ;;   (debugf "lift Op %s" @op)
       ;;   (is complete))
       ))))
