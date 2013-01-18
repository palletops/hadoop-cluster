(ns palletops.cluster.hadoop.cli
  "Command line interface for hadoop clusters."
  (:use
   [clojure.tools.cli :only [cli]]
   [clojure.tools.logging :only [errorf]]
   [palletops.cluster.hadoop.cli-impl :only [debug error exit log-level!]]))

(def commands #{:start :destroy :run})

(defn resolve-task
  [taskname]
  (try
    (let [ns (symbol (str "palletops.cluster.hadoop.cli." taskname))]
      (require ns)
      @(resolve (symbol (name ns) taskname)))
    (catch Exception e
      (errorf e (str "Could not resolve task '" taskname "'"))
      (throw (Exception. (str "Could not resolve task '" taskname "'") e)))))

(defn run-task
  [command opts args]
  ((resolve-task command) opts args))

;;; (run-task "start" {:A 1} [1 2 3])
;;; (run-task "start" {:spec-file "cluster_spec.clj"} [1 2 3])
;;; (run-task "start" {:spec-file "cluster_spec.clj" :profile "vb4"} [1 2 3])

(defn expiry
  []
  (doto (java.util.GregorianCalendar.
         (java.util.TimeZone/getTimeZone "GMT"))
    (. add java.util.Calendar/MONTH 1)))

(defn cli-args
  "Process command line arguments. Returns an option map, a vector of arguments
  and a help string."
  [args]
  (cli args
       ["-s" "--spec-file" "Specify the cluster layout"
        :default "cluster_spec.clj"]
       ["-c" "--credentials" "Specify credentials in a clj format file"
        :default "credentials.clj"]
       ["-v" "--verbose" "Output some verbose information" :flag true]
       ["-p" "--profile" "Pallet configuration profile to use"]
       ["-s" "--phases" "Phases to run"]
       ["-h" "--help" "Show this help message" :flag true]))

;;; (cli-args ["start"])
;;; (cli-args ["--spec-file" "cluster_spec.clj" "start"])

(def main-help
  (str "Control a hadoop cluster.

Supported commands

`start`
: Starts the cluster (specified by default in cluster_spec.clj)

`job job_spec.clj`
: Runs a job on the cluster

`destroy`
: Tears down the cluster

`Version`
: Output the CLI version and exit"
       \newline \newline
       (last (cli-args ["--help"]))))

(defn ^{:doc main-help} -main
  [& args]
  (let [[{:keys [verbose spec-file help] :as opts}
         [^String command & args]
         help-str]
        (cli-args args)]
    (when verbose
      (log-level! :debug))
    (debug "options" opts)
    (debug "command" command)
    (when (or help (not command))
      (println main-help)
      (flush)
      (exit 0))
    (let [date (java.util.Date.)]
      (when (.after date #=(expiry))
        (error
         "PalletOps CLI has expired, please contact palletops for a replacement")
        (System/exit 1)))
    (try
      (run-task command opts args)
      (catch Exception e
        (errorf e "Unexpected exception in task")
        (error (.getMessage e))))
    (exit 0)))


;;; (meta #'-main)
;;; (clojure.repl/doc -main)
;;; (debug "hello" 1 2 3)
