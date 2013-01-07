;;; If you want non-cluster wide credentials, put you s3 credentials in a
;;; function to generate urls with credntials. Note that this doesn't work
;;; with credentials containing a "/".

;; (defn s3n [path]
;;   (let [aws-key <your-aws-s3-key>
;;         aws-secret <your-aws-s3-secret>]
;;     (format "s3n://%s:%s@%s" aws-key aws-secret path)))

;;; As an alternative, you can specify s3 credentials via environment variables
;; (defn s3n [path]
;;   (let [aws-key (System/getenv "AWS_KEY")
;;         aws-secret (System/getenv "AWS_SECRET")]
;;     (format "s3n://%s:%s@%s" aws-key aws-secret path)))

{:steps
 [{:jar
   ;; The jarfile to run can be specified using pallet's remote-file options.
   ;; To specify a file already on the remote node, use :remote-file to specify
   ;; its path.
   ;; To specify a file on the local host, use :local-file to specify the local
   ;; path.
   ;; Use :url to specify a jar from the specified url.
   {:remote-file "/usr/local/hadoop-0.20.2/hadoop-examples-0.20.2-cdh3u5.jar"}
   :main "wordcount"
   :input ("s3n://pallet-play/hadoop-examples") ; used as first arg if present
   :output ("s3n://<your-dest-bucket>/<your-dest-directory>") ; used as second
                                                              ; arg if present
   ;; :args can be used to pass a sequence of command line arguments
   }]
 :on-completion :terminate-cluster}
