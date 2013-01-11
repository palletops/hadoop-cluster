{:steps
 [{:script-file "script/hello.sh"}
  {:script "echo hello inline script"}
  {:jar
   ;; The jarfile to run can be specified using pallet's remote-file options.
   ;; To specify a file already on the remote node, use :remote-file to specify
   ;; its path.
   ;; To specify a file on the local host, use :local-file to specify the local
   ;; path.
   ;; Use :url to specify a jar from the specified url.
   {:remote-file "/usr/local/hadoop-0.20.2/hadoop-examples-0.20.2-cdh3u5.jar"}
   :main "wordcount"
   :input "s3n://pallet-play/hadoop-examples" ; used as first arg if present
   :output "s3n://<your-dest-bucket>/<your-dest-directory>" ; used as second
                                                              ; arg if present
   ;; :args can be used to pass a sequence of command line arguments
   }]
 :on-completion :terminate-cluster}
