(ns palletops.cluster.hadoop-test
  (:use
   clojure.test
   palletops.cluster.hadoop
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
       [compute node-map node-types [:install
                                     :restart-collectd
                                     :configure
                                     :install-test
                                     :configure-test]]
       { ;; :collectd
        ;; (group-spec
        ;;  "collectd"
        ;;  :image image
        ;;  :count 1
        ;;  :extends [collectd-group])
        :namenode (assoc
                      (group-spec
                       :namenode
                       :extends [namenode-group hadoop-book-test])
                    :image image :count 1)
        :datanode (assoc datanode-group :image image :count 1)
        :graphite
        (group-spec
         "graphite"
         :image image
         :count 1
         :extends [collectd-group logging-group])}
       (let [op (lift [(map node-types [:namenode :graphite :datanode])]
                      :phase [:run-test :post-run]
                      :compute compute)]
         @op
         (is (complete? op)))))))
