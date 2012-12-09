(ns palletops.cluster.hadoop.cli.job
  "Task to run a job on a cluster. A job spec is a file containing a map with
`:steps` and `:on-completion` keys. Each step is a map of options for
hadoop-jar. `:on-completion` can be set to `:terminate-cluster` to destroy the
cluster on successful completion of the job."
  (:use
   [clojure.pprint :only [pprint]]
   [pallet.algo.fsmop :only [complete?]]
   [pallet.api :only [converge lift plan-fn server-spec]]
   [pallet.configure :only [compute-service]]
   [palletops.cluster.hadoop :only [hadoop-cluster]]
   [palletops.crate.hadoop :only [hadoop-jar]]
   [palletops.cluster.hadoop.cli-impl
    :only [debug error read-cluster-spec read-job-spec]]))

(defn step-server-spec
  [step-spec]
  (server-spec :phases {::run-jar (plan-fn (hadoop-jar step-spec))}))

(defn job
  "Job a cluster"
  [{:keys [spec-file profile] :as options} [job-spec-file & _]]
  (let [spec (read-cluster-spec spec-file)
        cluster (hadoop-cluster spec)
        service (compute-service profile)
        {:keys [on-completion] :as job-spec} (read-job-spec job-spec-file)
        step-specs (map step-server-spec (:steps job-spec))
        run-spec (server-spec :extends step-specs)
        groups (map
                #(update-in % [:phases] merge (:phases run-spec))
                (:groups cluster))]

    (debug "job-spec" (with-out-str (pprint job-spec)))
    (debug "groups" (with-out-str (pprint groups)))
    (if service
      (let [op (lift groups :compute service :phase [::run-jar])]
        @op
        (when-let [e (:exception @op)]
          (clojure.stacktrace/print-cause-trace e)
          (flush)
          (throw e))
        (when (and (complete? op) (= on-completion :terminate-cluster))
          (let [op (converge
                (map #(assoc % :count 0) (:groups cluster))
                :compute service)]
            @op
            (when-not (complete? op)
              (if-let [e (:exception @op)]
                (throw e)
                (error "Failed to destroy cluster"))))))
      (error "Could not find pallet profile" profile))
    (debug "job: lift complete")))