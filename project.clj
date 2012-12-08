(defproject hadoop-cluster "0.1.0-SNAPSHOT"
  :description "Hadoop cluster runner"
  :url "https://github.com/palletops/github-cluster"
  :license {:name "All rights reserved."}
  :dependencies [[org.cloudhoist/pallet "0.8.0-SNAPSHOT"]
                 [com.palletops/collectd-crate "0.1.0-SNAPSHOT"]
                 [com.palletops/graphite-crate "0.1.0-SNAPSHOT"]
                 [com.palletops/hadoop-crate "0.1.0-SNAPSHOT"]
                 [com.palletops/hadoop-config "0.1.0-SNAPSHOT"]
                 [org.cloudhoist/java "0.8.0-SNAPSHOT"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/clojure "1.4.0"]]
  :main palletops.cluster.hadoop.cli
  :repositories
  {"sonatype-snapshots" "https://oss.sonatype.org/content/repositories/snapshots"
   "sonatype" "https://oss.sonatype.org/content/repositories/releases/"})
