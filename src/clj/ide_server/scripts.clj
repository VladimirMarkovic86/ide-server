(ns ide-server.scripts
  (:require [mongo-lib.core :as mon]
            [utils-lib.core :as utils]))

(defn initialize-db
  "Initialize database"
  []
  (mon/mongodb-insert-many
    "language"
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
     { :code 31, :english "No entities", :serbian "Нема ентитета" }
     { :code 1032, :english "Build uberjar", :serbian "Изгради uberjar" }])
  (mon/mongodb-insert-many
    "role"
    [{ :role-name "User administrator", :functionalities [ "user-create", "user-read", "user-update", "user-delete" ] }
     { :role-name "User moderator", :functionalities [ "user-read", "user-update" ] }
     { :role-name "Language administrator", :functionalities [ "language-create", "language-read", "language-update", "language-delete" ] }
     { :role-name "Language moderator", :functionalities [ "language-read", "language-update" ] }
     { :role-name "Role administrator", :functionalities [ "role-create", "role-read", "role-update", "role-delete" ] }
     { :role-name "Role moderator", :functionalities [ "role-read", "role-update" ] }
     { :role-name "Project administrator", :functionalities [ "project-create", "project-read", "project-update", "project-delete" ] }
     { :role-name "Project moderator", :functionalities [ "project-read", "project-update" ] }
     { :role-name "Working area user", :functionalities [ "read-file", "execute-shell-command", "list-documents", "new-folder", "new-file", "move-document", "copy-document", "delete-document", "build-project", "build-uberjar", "build-project-dependencies", "clean-project", "run-project", "git-project", "git-status", "save-file-changes" ]}])
  (let [user-admin-id (:_id
                        (mon/mongodb-find-one
                          "role"
                          {:role-name "User administrator"}))
        language-admin-id (:_id
                            (mon/mongodb-find-one
                              "role"
                              {:role-name "Language administrator"}))
        role-admin-id (:_id
                        (mon/mongodb-find-one
                          "role"
                          {:role-name "Role administrator"}))
        project-admin-id (:_id
                           (mon/mongodb-find-one
                             "role"
                             {:role-name "Project administrator"}))
        working-area-user-id (:_id
                               (mon/mongodb-find-one
                                 "role"
                                 {:role-name "Working area user"}))
        encrypted-password (utils/encrypt-password
                             (or (System/getenv "ADMIN_USER_PASSWORD")
                                 "123"))]
    (mon/mongodb-insert-one
      "user"
      {:username "admin"
       :email "123@123"
       :password encrypted-password
       :roles [user-admin-id language-admin-id role-admin-id project-admin-id working-area-user-id]}))
  (let [user-id (:_id
                  (mon/mongodb-find-one
                    "user"
                    {}))]
    (mon/mongodb-insert-one
      "preferences"
      {:user-id user-id, :language "serbian", :language-name "Srpski" }))
 )

(defn initialize-db-if-needed
  "Check if database exists and initialize it if it doesn't"
  []
  (try
    (when-not (mon/mongodb-exists
                "language"
                {:english "Save"})
      (initialize-db))
    (catch Exception e
      (println e))
   ))

