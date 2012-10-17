(ns palletops.cluster.hadoop-test
  (:use
   clojure.test
   palletops.cluster.hadoop
   [pallet.api :only [group-spec plan-fn]]
   [pallet.live-test :only [images test-nodes]]))


(deftest ^:live-test live-test
  (let [settings {}]
    (doseq [image (images)]
      (test-nodes
       [compute node-map node-types [:install :configure]]
       {:collectd
        (group-spec
         "collectd"
         :image image
         :count 1
         :extends [collectd-group])
        :graphite
         (group-spec
          "graphite"
          :image image
          :count 1
          :extends [collectd-group logging-group])}))))
