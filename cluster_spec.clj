(def java-opts {:jmx-authenticate false :jmx-ssl false})


{:cluster-prefix "hc1"
 :groups {:master
          {:node-spec {:hardware {:hardware-id "m1.large"
                                  :min-ram 2048}}
           :count 1
           :roles #{:namenode :jobtracker}}
          :slave
          {:node-spec {:hardware {:hardware-id "m1.large"
                                  :min-ram 4096}}
           :count 2
           :roles #{:datanode :tasktracker}}
          ;; :graphite {:count 1 :roles #{:graphite}}
          }
 :node-spec {:image {:os-family :ubuntu
                     :os-version-matches "12.04"
                     :os-64-bit true}}
 :hadoop-settings
 {:dist :cloudera
  :dist-urls {:cloudera "http://3rd-party-dist.s3.amazonaws.com/"}
  :config
  {:namenode (merge {:jmx-port 3000} java-opts)
   :secondary-namenode (merge {:jmx-port 3001} java-opts)
   :jobtracker (merge {:jmx-port 3002} java-opts)
   :datanode (merge {:jmx-port 3003} java-opts)
   :tasktracker (merge {:jmx-port 3004} java-opts)}}}
