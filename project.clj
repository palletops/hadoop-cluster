(defproject com.palletops/hadoop-cluster "0.1.3-SNAPSHOT"
  :description "Hadoop cluster runner"
  :url "https://github.com/palletops/hadoop-cluster"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.palletops/pallet "0.8.0-RC.10"]
                 [com.palletops/collectd-crate "0.8.0-alpha.2"]
                 [com.palletops/graphite-crate "0.8.0-SNAPSHOT"]
                 [com.palletops/hadoop-crate "0.1.5-SNAPSHOT"]
                 [com.palletops/hadoop-config "0.1.1-SNAPSHOT"]
                 [com.palletops/java-crate "0.8.0-beta.4"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/clojure "1.6.0"]]
  :main palletops.cluster.hadoop.cli
  :test-selectors {:default (complement :live-test)
                   :live-test :live-test
                   :all (constantly true)})
