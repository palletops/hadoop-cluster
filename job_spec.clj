;;; Put you s3 credentials in environment variables
(defn s3n [path]
  (let [aws-key <your-aws-s3-key>
        aws-secret <your-aws-s3-secret>]
    (format "s3n://%s:%s@%s" aws-key aws-secret path)))

;;; As an alternative, put you s3 credentials directly in this file
;; (defn s3n [path]
;;   (str "s3n:<your key>:<your secret>@" path))

{:steps
 [{:jar {:remote-file
         "/usr/local/hadoop-0.20.2/hadoop-examples-0.20.2-cdh3u5.jar"}
   :main "wordcount"
   :input (s3n "pallet-play/hadoop-examples")
   :output (s3n "<your-dest-bucket>/<your-dest-directory>")}]
 :on-completion :terminate-cluster}
