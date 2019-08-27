(ns ide-server.scripts
  (:require [mongo-lib.core :as mon]
            [common-middle.collection-names :refer [db-updates-cname]]
            [common-server.scripts :as css]
            [ide-server.scripts.language :as issl]
            [ide-server.scripts.role :as issr]
            [ide-server.scripts.user :as issu]
            [ide-server.scripts.project :as issp]))

(defn initialize-db
  "Initialize database"
  []
  (css/initialize-db)
  (issl/insert-labels)
  (issr/insert-roles)
  (issu/update-users)
  ;;(issp/insert-projects)
  (mon/mongodb-insert-one
    db-updates-cname
    {:initialized true
     :date (java.util.Date.)})
 )

(defn initialize-db-if-needed
  "Check if database exists and initialize it if it doesn't"
  []
  (try
    (when-not (mon/mongodb-exists
                db-updates-cname
                {:initialized true})
      (initialize-db))
    (catch Exception e
      (println e))
   ))

