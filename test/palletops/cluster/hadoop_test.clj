(ns palletops.cluster.hadoop-test
  (:use
   clojure.test
   palletops.cluster.hadoop
   [clojure.tools.logging :only [debugf]]
   [pallet.algo.fsmop :only [complete?]]
   [pallet.api :only [group-spec lift plan-fn server-spec]]
   [pallet.live-test :only [images test-nodes]]
   [palletops.hadoop.hadoop-book-example
    :only [download-books import-books-to-hdfs run-books get-books-output]]))

(def hadoop-book-test
  (server-spec
   :phases {:install-test (plan-fn (download-books))
            :configure-test (plan-fn (import-books-to-hdfs))
            :run-test (plan-fn (run-books))
            :post-run (plan-fn (get-books-output))}))

(deftest ^:live-test live-test
  (let [settings {}]
    (doseq [image (images)]
      (test-nodes
       [compute node-map node-types
        [:install
         :collect-ssh-keys
         :configure :restart-collectd :run :init
         :install-test :configure-test]]
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
