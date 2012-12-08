(ns palletops.cluster.hadoop.cli.start
  "Task to start a cluster"
  (:use
   [clojure.pprint :only [pprint]]
   [pallet.api :only [converge]]
   [pallet.configure :only [compute-service]]
   [palletops.cluster.hadoop :only [hadoop-cluster]]
   [palletops.cluster.hadoop.cli-impl :only [debug error read-cluster-spec]]))

(defn start
  "Start a cluster"
  [{:keys [spec-file profile] :as options} args]
  (let [spec (read-cluster-spec spec-file)
        cluster (hadoop-cluster spec)
        service (compute-service profile)]
    (debug "groups" (with-out-str
                      (pprint (:groups cluster))))
    (if service
      (let [op (converge
                (:groups cluster)
                :compute service
                :phase [:install
                        :collect-ssh-keys
                        :configure
                        :restart-collectd
                        :run :init
                        :install-test
                        :configure-test])]
        @op)
      (error "Could not find pallet profile" profile))
    (debug "start: converge complete")))
