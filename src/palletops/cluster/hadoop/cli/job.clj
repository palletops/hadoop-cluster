(ns palletops.cluster.hadoop.cli.job
  "Task to run a job on a cluster. A job spec is a file containing a map with
`:steps` and `:on-completion` keys. Each step is a map of options for
hadoop-jar. `:on-completion` can be set to `:terminate-cluster` to destroy the
cluster on successful completion of the job."
  (:use
   [clojure.pprint :only [pprint]]
   [pallet.actions :only [exec-checked-script]]
   [pallet.algo.fsmop :only [complete?]]
   [pallet.api :only [converge lift plan-fn server-spec]]
   [pallet.configure :only [compute-service compute-service-from-map]]
   [pallet.crate :only [phase-context]]
   [palletops.cluster.hadoop :only [hadoop-cluster]]
   [palletops.crate.hadoop :only [hadoop-jar]]
   [palletops.cluster.hadoop.cli-impl
    :only [debug error read-cluster-spec read-credentials read-job-spec]]))

(defn step-server-spec
  [step-spec index]
  (cond
   (:jar step-spec)
   (server-spec
    :phases
    {::run-step (plan-fn
                  (phase-context (str "step " index " - hadoop jar") {}
                    (hadoop-jar step-spec)))})

   (:script step-spec)
   (server-spec
    :phases
    {::run-step (plan-fn
                  (phase-context (str "step " index " - inline script") {}
                    (exec-checked-script
                     "job step"
                     ~(:script step-spec))))})

   (:script-file step-spec)
   (server-spec
    :phases
    {::run-step (plan-fn
                  (phase-context
                      (str "step " index " - " (:script-file step-spec)) {}
                    (exec-checked-script
                     "job step"
                     ~(slurp (:script-file step-spec)))))})))

(defn job
  "Job a cluster"
  [{:keys [credentials spec-file profile] :as options} [job-spec-file & _]]
  (let [spec (read-cluster-spec spec-file)
        cluster (hadoop-cluster spec)
        service (if profile
                  (compute-service profile)
                  (compute-service-from-map (read-credentials credentials)))
        {:keys [on-completion] :as job-spec} (read-job-spec job-spec-file)
        step-specs (map step-server-spec (:steps job-spec) (range))
        run-spec (server-spec :extends step-specs)
        groups (map
                #(update-in % [:phases] merge (:phases run-spec))
                (:groups cluster))]

    (debug "job-spec" (with-out-str (pprint job-spec)))
    (debug "groups" (with-out-str (pprint groups)))
    (if service
      (let [op (lift groups :compute service :phase [::run-step])]
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
