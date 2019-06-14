(ns ide-server.scripts.language
  (:require [mongo-lib.core :as mon]
            [common-middle.collection-names :refer [language-cname]]))

(defn insert-labels
  "Inserts labels"
  []
  (mon/mongodb-update-one
    language-cname
    {:code 62}
    {:$set
      {:english "Integrated Development Environment app"
       :serbian "Интеграционо Развојно Окружење"}})
  (mon/mongodb-update-one
    language-cname
    {:code 63}
    {:$set
      {:english "Integrated Development Environment app is based on sample app, and it implements text editor, git syncronization, build tool and server actions."
       :serbian "Интеграционо Развојно Окружење је апликација заснована на sample app пројекту, и имплементира текстуално уређивање, git синхронизацију, алат за изградњу и рад са сервером."}})
  (mon/mongodb-update-one
    language-cname
    {:code 81}
    {:$set
      {:english "Integrated Development Environment App reset password"
       :serbian "Integrated Development Environment App промена лозинке"}})
  (mon/mongodb-update-one
    language-cname
    {:code 82}
    {:$set
      {:english "A password reset was requested for Integrated Development Environment App account with this email address.<br>To continue password reset copy, paste and confirm code from below."
       :serbian "Налог апликације Integrated Development Environment App са овом е-адресом захтева промену лозинке.<br>Да би наставили промену лозинке копирајте, налепите и потврдите следећи код."}})
  (mon/mongodb-insert-many
    language-cname
    [{ :code 1001, :english "Project", :serbian "Пројекат" }
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
     { :code 1032, :english "Build uberjar", :serbian "Изгради uberjar" }
     { :code 1033, :english "Application", :serbian "Апликација" }
     { :code 1034, :english "Library", :serbian "Библиотека" }
     { :code 1035, :english "Task code", :serbian "Код задатка" }
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
     { :code 1071, :english "Clojure", :serbian "Clojure" }
     { :code 1072, :english "Clojure/Script", :serbian "Clojure/Script" }
     { :code 1073, :english "ClojureScript", :serbian "ClojureScript" }
     { :code 1074, :english "Application", :serbian "Апликација" }
     { :code 1075, :english "Library", :serbian "Библиотека" }
     { :code 1076, :english "Project entity", :serbian "Ентитет пројекат" }
     { :code 1077, :english "Task entity", :serbian "Ентитет задатак" }
	    ]))

