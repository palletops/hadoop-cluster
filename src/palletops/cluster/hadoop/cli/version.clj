(ns palletops.cluster.hadoop.cli.version
  "Task to display CLI version"
  (:use
   [clojure.java.io :only [reader resource]]))

(def pom-properties
  "META-INF/maven/hadoop-cluster/hadoop-cluster/pom.properties")

(defn version
  "Display CLI version, and exit"
  [options args]
  (let [properties (doto (java.util.Properties.)
                        (.load (reader (resource pom-properties))))]
    (println "Version:" (.get properties "version"))
    (println "Revision:" (.get properties "revision"))
    (flush)))
