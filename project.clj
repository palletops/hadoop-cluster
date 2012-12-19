(defproject hadoop-cluster "0.1.0-SNAPSHOT"
  :description "Hadoop cluster runner"
  :url "https://github.com/palletops/github-cluster"
  :license {:name "All rights reserved."}
  :dependencies [[org.cloudhoist/pallet "0.8.0-SNAPSHOT"]
                 [com.palletops/collectd-crate "0.1.0"]
                 [com.palletops/graphite-crate "0.1.0"]
                 [com.palletops/hadoop-crate "0.1.1-SNAPSHOT"]
                 [com.palletops/hadoop-config "0.1.0-SNAPSHOT"]
                 [org.cloudhoist/java "0.8.0-alpha.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/clojure "1.4.0"]]
  :main palletops.cluster.hadoop.cli
  :repositories
  {"sonatype-snapshots" "https://oss.sonatype.org/content/repositories/snapshots/"
   "sonatype" "https://oss.sonatype.org/content/repositories/releases/"}
  :test-selectors {:default (complement :live-test)
                   :live-test :live-test
                   :all (constantly true)})
