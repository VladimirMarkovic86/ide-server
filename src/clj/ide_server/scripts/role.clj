(ns ide-server.scripts.role
  (:require [mongo-lib.core :as mon]
            [common-middle.collection-names :refer [role-cname]]
            [ide-middle.functionalities :as imfns]
            [ide-middle.role-names :refer [project-admin-rname
                                           project-mod-rname
                                           task-admin-rname
                                           task-mod-rname
                                           working-area-user-rname]]))

(defn insert-roles
  "Inserts roles"
  []
  (mon/mongodb-insert-many
    role-cname
    [{:role-name project-admin-rname
      :functionalities [imfns/project-create
                        imfns/project-read
                        imfns/project-update
                        imfns/project-delete]}
     {:role-name project-mod-rname
      :functionalities [imfns/project-read
                        imfns/project-update]}
     {:role-name task-admin-rname
      :functionalities [imfns/task-create
                        imfns/task-read
                        imfns/task-update
                        imfns/task-delete]}
     {:role-name task-mod-rname
      :functionalities [imfns/task-read
                        imfns/task-update]}
     {:role-name working-area-user-rname
      :functionalities [imfns/read-file
                        imfns/execute-shell-command
                        imfns/list-documents
                        imfns/new-folder
                        imfns/new-file
                        imfns/move-document
                        imfns/copy-document
                        imfns/delete-document
                        imfns/build-project
                        imfns/build-uberjar
                        imfns/build-project-dependencies
                        imfns/clean-project
                        imfns/run-project
                        imfns/git-project
                        imfns/git-status
                        imfns/git-diff
                        imfns/git-log
                        imfns/git-unpushed
                        imfns/git-commit-push
                        imfns/git-file-change-state
                        imfns/git-commit-push-action
                        imfns/save-file-changes
                        imfns/versioning-project
                        imfns/upgrade-versions
                        imfns/upgrade-versions-save
                        imfns/upgrade-versions-build
                        imfns/find-text-in-files
                        imfns/projects-tree
                        ]}
     ]))

