{:dev
   {:dependencies
    [[org.cloudhoist/pallet-vmfest "0.2.1-SNAPSHOT"]
     [org.cloudhoist/pallet "0.8.0-SNAPSHOT" :classifier "tests"]
     [com.palletops/hadoop-book-example "0.1.0-SNAPSHOT"]
     [ch.qos.logback/logback-classic "1.0.0"]]
    :test-selectors {:default (complement :live-test)
                     :live-test :live-test
                     :all (constantly true)}}}
