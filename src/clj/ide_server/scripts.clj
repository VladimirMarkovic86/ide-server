(ns ide-server.scripts
  (:require [mongo-lib.core :as mon]
            [utils-lib.core :as utils]
            [common-middle.collection-names :refer [db-updates-cname
                                                    language-cname
                                                    role-cname
                                                    user-cname
                                                    preferences-cname]]
            [ide-middle.collection-names :refer [project-cname]]
            [common-middle.role-names :refer [user-admin-rname
                                              user-mod-rname
                                              language-admin-rname
                                              language-mod-rname
                                              role-admin-rname
                                              role-mod-rname]]
            [ide-middle.role-names :refer [project-admin-rname
                                           project-mod-rname
                                           task-admin-rname
                                           task-mod-rname
                                           working-area-user-rname]]
            [common-middle.functionalities :as fns]
            [ide-middle.functionalities :as imfns]))

(defn initialize-db
  "Initialize database"
  []
  (mon/mongodb-insert-many
    language-cname
    [{ :code 1, :english "Save", :serbian "Сачувај" }
     { :code 2, :english "Log out", :serbian "Одјави се" }
     { :code 3, :english "Home", :serbian "Почетна" }
     { :code 4, :english "Create", :serbian "Креирај" }
     { :code 5, :english "Show all", :serbian "Прикажи све" }
     { :code 6, :english "Details", :serbian "Детаљи" }
     { :code 7, :english "Edit", :serbian "Измени" }
     { :code 8, :english "Delete", :serbian "Обриши" }
     { :code 9, :english "Actions", :serbian "Акције" }
     { :code 10, :english "Insert", :serbian "Упиши" }
     { :code 11, :english "Update", :serbian "Ажурирај" }
     { :code 12, :english "Cancel", :serbian "Откажи" }
     { :code 13, :english "Search", :serbian "Претрага" }
     { :code 14, :english "E-mail", :serbian "Е-пошта" }
     { :code 15, :english "Password", :serbian "Лозинка" }
     { :code 16, :english "Remember me", :serbian "Упамти ме" }
     { :code 17, :english "Log in", :serbian "Пријави се" }
     { :code 18, :english "Sign up", :serbian "Направи налог" }
     { :code 19, :english "Username", :serbian "Корисничко име" }
     { :code 20, :english "Confirm password", :serbian "Потврди лозинку" }
     { :code 21, :english "User", :serbian "Корисник" }
     { :code 22, :english "Role", :serbian "Улога" }
     { :code 23, :english "Language", :serbian "Језик" }
     { :code 24, :english "Label code", :serbian "Код лабеле" }
     { :code 25, :english "English", :serbian "Енглески" }
     { :code 26, :english "Serbian", :serbian "Српски" }
     { :code 1001, :english "Project", :serbian "Пројекат" }
     { :code 1002, :english "Working area", :serbian "Радионица" }
     { :code 1003, :english "Name", :serbian "Назив" }
     { :code 1004, :english "Group id", :serbian "ИД групе" }
     { :code 1005, :english "Artfact id", :serbian "ИД артифакта" }
     { :code 1006, :english "Version", :serbian "Верзија" }
     { :code 1007, :english "Absolute path", :serbian "Апсолутна путања" }
     { :code 1008, :english "Git remote link", :serbian "Гит удаљена веза" }
     { :code 1009, :english "Programming language", :serbian "Програмски језик" }
     { :code 1010, :english "Project type", :serbian "Тип пројекта" }
     { :code 1011, :english "Shell", :serbian "Шел" }
     { :code 1012, :english "File system", :serbian "Фајл систем" }
     { :code 1013, :english "Leiningen", :serbian "Лајнинген" }
     { :code 1014, :english "Git", :serbian "Гит" }
     { :code 1015, :english "IDE", :serbian "ИРО" }
     { :code 1016, :english "Output", :serbian "Излаз" }
     { :code 1017, :english "Error", :serbian "Грешка" }
     { :code 1018, :english "Build", :serbian "Изгради" }
     { :code 1019, :english "Build all", :serbian "Изгради све" }
     { :code 1020, :english "Clean", :serbian "Очисти" }
     { :code 1021, :english "Start", :serbian "Покрени" }
     { :code 1022, :english "Stop", :serbian "Заустави" }
     { :code 1023, :english "Restart", :serbian "Поново покрени" }
     { :code 1024, :english "Status", :serbian "Стање" }
     { :code 1025, :english "Differences", :serbian "Разлике" }
     { :code 1026, :english "Save all", :serbian "Сачувај све" }
     { :code 1027, :english "New folder", :serbian "Нова фасцикла" }
     { :code 1028, :english "New file", :serbian "Нови фајл" }
     { :code 1029, :english "Cut", :serbian "Премести" }
     { :code 1030, :english "Copy", :serbian "Копирај" }
     { :code 1031, :english "Paste", :serbian "Овде копирај/премести" }
     { :code 27, :english "Functionality", :serbian "Функционалност" }
     { :code 28, :english "Role name", :serbian "Назив улоге" }
     { :code 29, :english "Functionalities", :serbian "Функционалности" }
     { :code 30, :english "Roles", :serbian "Улоге" }
     { :code 31, :english "No entities", :serbian "Нема ентитета" }])
  (mon/mongodb-insert-many
    role-cname
    [{:role-name user-admin-rname
      :functionalities [fns/user-create
                        fns/user-read
                        fns/user-update
                        fns/user-delete]}
     {:role-name user-mod-rname
      :functionalities [fns/user-read
                        fns/user-update]}
     {:role-name language-admin-rname
      :functionalities [fns/language-create
                        fns/language-read
                        fns/language-update
                        fns/language-delete]}
     {:role-name language-mod-rname
      :functionalities [fns/language-read
                        fns/language-update]}
     {:role-name role-admin-rname
      :functionalities [fns/role-create
                        fns/role-read
                        fns/role-update
                        fns/role-delete]}
     {:role-name role-mod-rname
      :functionalities [fns/role-read
                        fns/role-update]}
     {:role-name project-admin-rname
      :functionalities [imfns/project-create
                        imfns/project-read
                        imfns/project-update
                        imfns/project-delete]}
     {:role-name project-mod-rname
      :functionalities [imfns/project-read
                        imfns/project-update]}
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
                        ]}])
  (let [user-admin-id (:_id
                        (mon/mongodb-find-one
                          role-cname
                          {:role-name user-admin-rname}))
        language-admin-id (:_id
                            (mon/mongodb-find-one
                              role-cname
                              {:role-name language-admin-rname}))
        role-admin-id (:_id
                        (mon/mongodb-find-one
                          role-cname
                          {:role-name role-admin-rname}))
        project-admin-id (:_id
                           (mon/mongodb-find-one
                             role-cname
                             {:role-name project-admin-rname}))
        working-area-user-id (:_id
                               (mon/mongodb-find-one
                                 role-cname
                                 {:role-name working-area-user-rname}))
        encrypted-password (utils/encrypt-password
                             (or (System/getenv "ADMIN_USER_PASSWORD")
                                 "123"))]
    (mon/mongodb-insert-one
      user-cname
      {:username "admin"
       :email "123@123"
       :password encrypted-password
       :roles [user-admin-id
               language-admin-id
               role-admin-id
               project-admin-id
               working-area-user-id]}))
  (let [user-id (:_id
                  (mon/mongodb-find-one
                    user-cname
                    {:username "admin"}))]
    (mon/mongodb-insert-one
      preferences-cname
      {:user-id user-id
       :language "serbian"
       :language-name "Srpski" }))
  (mon/mongodb-insert-one
    db-updates-cname
    {:initialized true
     :date (java.util.Date.)})
 )

(defn db-update-1
  "Database update 1"
  []
  (mon/mongodb-insert-many
    language-cname
    [{ :code 1032, :english "Build uberjar", :serbian "Изгради uberjar" }
     { :code 1033, :english "Application", :serbian "Апликација" }
     { :code 1034, :english "Library", :serbian "Библиотека" }
     ])
  (mon/mongodb-insert-one
    db-updates-cname
    {:update 1
     :date (java.util.Date.)})
 )

(defn db-update-2
  "Database update 2"
  []
  (mon/mongodb-insert-many
    language-cname
    [{ :code 1035, :english "Task code", :serbian "Код задатка" }
     { :code 1036, :english "Description", :serbian "Опис" }
     { :code 1037, :english "Type", :serbian "Тип" }
     { :code 1038, :english "Priority", :serbian "Приоритет" }
     { :code 1039, :english "Difficulty", :serbian "Тежина" }
     { :code 1040, :english "Status", :serbian "Статус" }
     { :code 1041, :english "Estimated time", :serbian "Процена времена" }
     { :code 1042, :english "Time taken", :serbian "Потрошено времена" }
     { :code 1043, :english "Task", :serbian "Задатак" }
     { :code 1044, :english "Bug", :serbian "Грешка" }
     { :code 1045, :english "New functionality", :serbian "Нова функционалност" }
     { :code 1046, :english "Refactoring", :serbian "Рефакторисање" }
     { :code 1047, :english "Low", :serbian "Низак" }
     { :code 1048, :english "Medium", :serbian "Средњи" }
     { :code 1049, :english "High", :serbian "Висок" }
     { :code 1050, :english "Easy", :serbian "Лако" }
     { :code 1051, :english "Medium", :serbian "Средња" }
     { :code 1052, :english "Hard", :serbian "Тешко" }
     { :code 1053, :english "Open", :serbian "Отворен" }
     { :code 1054, :english "Development", :serbian "Развој" }
     { :code 1055, :english "Deployed", :serbian "Отпремљено" }
     { :code 1056, :english "Testing", :serbian "Тестирање" }
     { :code 1057, :english "Rejected", :serbian "Одбијено" }
     { :code 1058, :english "Done", :serbian "Готово" }
     { :code 1059, :english "Versioning", :serbian "Верзионисање" }
     { :code 1060, :english "Commit", :serbian "Комитуј" }
     { :code 1061, :english "Commit and push", :serbian "Комитуј и проследи" }
     { :code 1062, :english "Push", :serbian "Проследи" }
     { :code 1063, :english "Commit message", :serbian "Комит порука" }
     { :code 1064, :english "Change differences", :serbian "Измењено" }
     { :code 1065, :english "Unpushed commits", :serbian "Не прослеђени комитови" }
     { :code 1066, :english "Set remote", :serbian "Постави удаљени" }
     { :code 1067, :english "Log", :serbian "Лог" }
     { :code 1068, :english "Find in file", :serbian "Нађи у фајлу" }
     { :code 1069, :english "Find", :serbian "Пронађи" }
     { :code 1070, :english "Upgrade versions", :serbian "Надогради верзије" }
     ])
  (mon/mongodb-insert-many
    role-cname
    [{:role-name task-admin-rname
      :functionalities [imfns/task-create
                        imfns/task-read
                        imfns/task-update
                        imfns/task-delete]}
     {:role-name task-mod-rname
      :functionalities [imfns/task-read
                        imfns/task-update]}])
  (let [user (mon/mongodb-find-one
               user-cname
               {:username "admin"})
        task-admin-id (:_id
                        (mon/mongodb-find-one
                          role-cname
                          {:role-name task-admin-rname}))
        user (update-in
               user
               [:roles]
               conj
               task-admin-id)
        user-id (:_id user)
        user (dissoc
               user
               :_id)]
    (mon/mongodb-update-by-id
      user-cname
      user-id
      user))
  (mon/mongodb-insert-one
    db-updates-cname
    {:update 2
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
    (when-not (mon/mongodb-exists
                db-updates-cname
                {:update 1})
      (db-update-1))
    (when-not (mon/mongodb-exists
                db-updates-cname
                {:update 2})
      (db-update-2))
    (catch Exception e
      (println e))
   ))

