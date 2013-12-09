(defproject com.palletops/hadoop-cluster "0.1.2-SNAPSHOT"
  :description "Hadoop cluster runner"
  :url "https://github.com/palletops/github-cluster"
  :license {:name "All rights reserved."}
  :dependencies [[com.palletops/pallet "0.8.0-RC.4"]
                 [com.palletops/collectd-crate "0.8.0-alpha.2"]
                 [com.palletops/graphite-crate "0.8.0-alpha.1"]
                 [com.palletops/hadoop-crate "0.1.5-SNAPSHOT"]
                 [com.palletops/hadoop-config "0.1.1-SNAPSHOT"]
                 [com.palletops/java-crate "0.8.0-beta.4"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/clojure "1.4.0"]]
  :main palletops.cluster.hadoop.cli
  :repositories
  {"sonatype-snapshots"
   {:url "https://oss.sonatype.org/content/repositories/snapshots/"}
   "sonatype"
   {:url "https://oss.sonatype.org/content/repositories/releases/"}}
  :test-selectors {:default (complement :live-test)
                   :live-test :live-test
                   :all (constantly true)})
