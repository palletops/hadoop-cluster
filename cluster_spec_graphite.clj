;;; A cluster configuration with a Graphite monitor

;;; Common options are factored out to avoid repetition
(def java-opts
  {:jmx-authenticate false
   :jmx-ssl false})

;;; A more complex cluster specification
{:cluster-prefix "hc2"
 :groups {:master
          {:node-spec {:hardware {:hardware-id "m1.medium"}}
           :count 1
           :roles #{:namenode :jobtracker}}
          :slave
          {:node-spec {:hardware {:hardware-id "m1.medium"}}
           :count 2
           :roles #{:datanode :tasktracker}}
          ;; the graphite node will run the graphite web server
          :graphite
          {:node-spec {:hardware {:hardware-id "m1.small"}}
           :count 1
           :roles #{:graphite}}}

 :node-spec {:image {:os-family :ubuntu
                     :os-version-matches "12.04"
                     :os-64-bit true}}
 :hadoop-settings
 {:dist :cloudera
  :dist-urls {:cloudera "http://3rd-party-dist.s3.amazonaws.com/"}
  ;; here we specify role specific configuration of JMX ports for
  ;; monitoring the hadoop processes
  :config
  {:namenode (merge {:jmx-port 3000} java-opts)
   :secondary-namenode (merge {:jmx-port 3001} java-opts)
   :jobtracker (merge {:jmx-port 3002} java-opts)
   :datanode (merge {:jmx-port 3003} java-opts)
   :tasktracker (merge {:jmx-port 3004} java-opts)}}}
