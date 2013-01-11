(defbrick hadoop-cluster
  :tools [:git :lein :bash]
  :tool {:lein {:profiles [:default :palletops]}})
