(ns palletops.cluster.hadoop.cli.destroy
  "Task to destroy a cluster"
  (:use
   [clojure.pprint :only [pprint]]
   [pallet.api :only [converge]]
   [pallet.configure :only [compute-service]]
   [palletops.cluster.hadoop :only [hadoop-cluster]]
   [palletops.cluster.hadoop.cli-impl :only [debug error read-cluster-spec]]))

(defn destroy
  "Destroy a cluster"
  [{:keys [spec-file profile] :as options} args]
  (let [spec (read-cluster-spec spec-file)
        cluster (hadoop-cluster spec)
        service (compute-service profile)]
    (debug "groups" (with-out-str
                      (pprint (:groups cluster))))
    (if service
      (let [op (converge
                (map #(assoc % :count 0) (:groups cluster))
                :compute service)]
        @op)
      (error "Could not find pallet profile" profile))
    (debug "destroy: converge complete")))
