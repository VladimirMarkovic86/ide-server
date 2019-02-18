(ns ide-server.scripts.user
  (:require [mongo-lib.core :as mon]
            [common-middle.collection-names :refer [user-cname
                                                    role-cname]]
            [ide-middle.role-names :refer [project-admin-rname
                                           task-admin-rname
                                           working-area-user-rname]]))

(defn update-users
  "Updates users"
  []
  (let [project-admin-id (:_id
                           (mon/mongodb-find-one
                             role-cname
                             {:role-name project-admin-rname}))
        task-admin-id (:_id
                        (mon/mongodb-find-one
                          role-cname
                          {:role-name task-admin-rname}))
        working-area-user-id (:_id
                               (mon/mongodb-find-one
                                 role-cname
                                 {:role-name working-area-user-rname}))
        ide-roles [project-admin-id
                   task-admin-id
                   working-area-user-id]]
    (mon/mongodb-update-one
      user-cname
      {:username "admin"}
      {:$addToSet
        {:roles
          {:$each ide-roles}}
       })
    (mon/mongodb-update-one
      user-cname
      {:username "guest"}
      {:$addToSet
        {:roles
          {:$each ide-roles}}
       }))
 )

