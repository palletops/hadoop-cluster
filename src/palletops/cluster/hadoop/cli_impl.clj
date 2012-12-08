(ns palletops.cluster.hadoop.cli-impl
  "Implementation for the haoop cli"
  (:use
   [clojure.java.io :only [file]]))

(defn eprintln [& args]
  (binding [*out* *err*]
    (apply println args)))

(def log-level nil)

(defn log-level! [level]
  (alter-var-root #'log-level (constantly level)))

(defn debug
  [& args]
  (when (= :debug log-level)
    (apply println args)))

(defn error
  [& args]
  (let [[options args] (if (map? (first args))
                         [(first args) (rest args)]
                         [nil args])]
    (apply eprintln args)
    (shutdown-agents)
    (System/exit (:exit-code options 1))))

(defn check-readable
  "Ensure the specified file exists, and is readable. Exits otherwise."
  [path]
  (when-not (.exists (file path))
    (error "File" path "does not exist"))
  (when-not (.canRead (file path))
    (error "File" path "can not be read")))

(defn read-cluster-spec
  "Return the cluster spec at the specified path."
  [path]
  (check-readable path)
  (load-file path))

(defn read-job-spec
  "Return the job spec at the specified path."
  [path]
  (check-readable path)
  (load-file path))
