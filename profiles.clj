{:jclouds
 {:dependencies
  [[com.palletops/pallet-jclouds "1.7.3"]
   [org.apache.jclouds.provider/aws-ec2 "1.7.2"]
   [org.apache.jclouds.provider/aws-s3 "1.7.2"]
   [org.apache.jclouds.driver/jclouds-slf4j "1.7.2"
    :exclusions [org.slf4j/slf4j-api]]
   [org.apache.jclouds.driver/jclouds-sshj "1.7.2"]]},
 :dev
 {:dependencies
  [[com.palletops/pallet "0.8.0-RC.10" :classifier "tests"]
   [com.palletops/pallet-vmfest "0.4.0-alpha.1"]
   [org.clojars.tbatchelli/vboxjxpcom "4.2.4"]
   [com.palletops/pallet-lein "0.6.0-beta.9"]
   [com.palletops/hadoop-book-example "0.1.1"]
   [ch.qos.logback/logback-classic "1.0.9"]
   [org.slf4j/jcl-over-slf4j "1.6.6"]],
  :plugins [[lein-pallet-release "RELEASE"]]}}
