(ns palletops.cluster.hadoop.cli.destroy
  "Task to destroy a cluster"
  (:use
   [clojure.pprint :only [pprint]]
   [pallet.api :only [converge]]
   [pallet.configure :only [compute-service compute-service-from-map]]
   [palletops.cluster.hadoop :only [hadoop-cluster]]
   [palletops.cluster.hadoop.cli-impl
    :only [debug error read-cluster-spec read-credentials]]))

(defn destroy
  "Destroy a cluster"
  [{:keys [credentials spec-file profile] :as options} args]
  (let [spec (read-cluster-spec spec-file)
        cluster (hadoop-cluster spec)
        service (if profile
                  (compute-service profile)
                  (compute-service-from-map (read-credentials credentials)))]
    (debug "groups" (with-out-str
                      (pprint (:groups cluster))))
    (if service
      (let [op (converge
                (map #(assoc % :count 0) (:groups cluster))
                :compute service)]
        @op)
      (error "Could not find pallet profile" profile))
    (debug "destroy: converge complete")))
