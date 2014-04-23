{:dev
 {:dependencies
  [;;[com.palletops/pallet-vmfest "0.4.0-alpha.1"]
   [com.palletops/pallet "0.8.0-RC.9" :classifier "tests"]
   [com.palletops/pallet-lein "0.6.0-beta.9"]
   [com.palletops/hadoop-book-example "0.1.1"]
   [ch.qos.logback/logback-classic "1.0.9"]
   [org.slf4j/jcl-over-slf4j "1.6.6"]]}
  :jclouds
 {:dependencies
  [[com.palletops/pallet-jclouds "1.7.3"]
   [org.apache.jclouds.provider/aws-ec2 "1.7.2"]
   [org.apache.jclouds.provider/aws-s3 "1.7.2"]
   [org.apache.jclouds.driver/jclouds-slf4j "1.7.2"
    ;; the declared version is old and can overrule the resolved version
    :exclusions [org.slf4j/slf4j-api]]
   [org.apache.jclouds.driver/jclouds-sshj "1.7.2"]]}}
