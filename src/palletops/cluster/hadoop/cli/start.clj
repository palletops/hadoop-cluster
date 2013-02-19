(ns palletops.cluster.hadoop.cli.start
  "Task to start a cluster"
  (:use
   [clojure.pprint :only [pprint]]
   [clojure.stacktrace :only [print-cause-trace]]
   [clojure.string :only [split]]
   [pallet.algo.fsmop :only [complete?]]
   [pallet.api :only [converge]]
   [pallet.configure :only [compute-service compute-service-from-map]]
   [pallet.core.api :only [phase-errors]]
   [palletops.cluster.hadoop :only [hadoop-cluster]]
   [palletops.cluster.hadoop.cli-impl
    :only [debug error print-cluster read-cluster-spec read-credentials]]))

(defn start
  "Start a cluster"
  [{:keys [spec-file credentials profile phases] :as options} args]
  (let [spec (read-cluster-spec spec-file)
        cluster (hadoop-cluster spec)
        service (if profile
                  (compute-service profile)
                  (compute-service-from-map (read-credentials credentials)))]
    (debug "groups" (with-out-str (pprint (:groups cluster))))
    (debug "spec" (with-out-str (pprint spec)))
    (if service
      (let [op (converge
                (:groups cluster)
                :compute service
                :phase (if phases
                         (map keyword (split phases #","))
                         [:install
                          :collect-ssh-keys
                          :configure
                          :restart-collectd
                          :run :init
                          :install-test
                          :configure-test]))]
        @op
        (if (complete? op)
          (print-cluster @op)
          (do
            (println "An error occured")
            (when-let [e (:exception @op)]
              (print-cause-trace e))
            (when-let [e (seq (phase-errors op))]
              (pprint (->> e (map :error) (map #(dissoc % :type)))))
            (println "See logs for further details")))
        (flush))
      (error "Could not find pallet profile" profile))
    (debug "start: converge complete")))
