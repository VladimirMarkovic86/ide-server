(ns ide-server.scripts.project
  (:require [mongo-lib.core :as mon]
            [ide-middle.collection-names :refer [project-cname]]))

(defn insert-projects
  "Inserts projects"
  []
  (mon/mongodb-insert-many
    project-cname
    [{
	     :name "Data access object library"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/dao_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/dao-lib.git"
	     :artifact-id "dao-lib"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :version "0.1.0"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Integrated development environment server"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "ide-server"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/ide_server"
	     :git-remote-link "git@github.com:VladimirMarkovic86/ide-server.git"
	     :language "clojure"
	     :project-type "application"
     }
     {
	     :name "Mongo library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "mongo-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/mongo_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/mongo-lib.git"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Optical character recognition library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "ocr-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/ocr_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/ocr-lib.git"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Optical character recognition server"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "ocr-server"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/ocr_server"
	     :git-remote-link "git@github.com:VladimirMarkovic86/ocr-server.git"
	     :language "clojure"
	     :project-type "application"
     }
     {
	     :name "Personal organiser server"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "personal-organiser-server"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/personal_organiser_server"
	     :git-remote-link "git@github.com:VladimirMarkovic86/personal-organiser-server.git"
	     :language "clojure"
	     :project-type "application"
     }
     {
	     :name "Sample server"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "sample-server"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/sample_server"
	     :git-remote-link "git@github.com:VladimirMarkovic86/sample-server.git"
	     :language "clojure"
	     :project-type "application"
     }
     {
	     :name "Server library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "server-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/server_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/server-lib.git"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Session library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "session-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/session_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/session-lib.git"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Srpski jezik library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "srpski-jezik-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/srpski_jezik_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/srpski-jezik-lib.git"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Ajax library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "ajax-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/ajax_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/ajax-lib.git"
	     :language "clojure_script"
	     :project-type "library"
     }
     {
	     :name "Client test library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "client-test-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/client_test_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/client-test-lib.git"
	     :language "clojurescript"
	     :project-type "library"
     }
     {
	     :name "Framework library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "framework-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/framework_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/framework-lib.git"
	     :language "clojurescript"
	     :project-type "library"
     }
     {
	     :name "HTML CSS library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "htmlcss-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/htmlcss_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/htmlcss-lib.git"
	     :language "clojurescript"
	     :project-type "library"
     }
     {
	     :name "Integrated development client"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "ide-client"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/ide_client"
	     :git-remote-link "git@github.com:VladimirMarkovic86/ide-client.git"
	     :language "clojurescript"
	     :project-type "application"
     }
     {
	     :name "JavaScript library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "js-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/js_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/js-lib.git"
	     :language "clojurescript"
	     :project-type "library"
     }
     {
	     :name "Optical character recognition client"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "ocr-client"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/ocr_client"
	     :git-remote-link "git@github.com:VladimirMarkovic86/ocr-client.git"
	     :language "clojurescript"
	     :project-type "application"
     }
     {
	     :name "Personal organiser client"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "personal-organiser-client"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/personal_organiser_client"
	     :git-remote-link "git@github.com:VladimirMarkovic86/personal-organiser-client.git"
	     :language "clojurescript"
	     :project-type "application"
     }
     {
	     :name "Sample client"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "sample-client"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/sample_client"
	     :git-remote-link "git@github.com:VladimirMarkovic86/sample-client.git"
	     :language "clojurescript"
	     :project-type "application"
     }
     {
	     :name "Utils library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "utils-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/utils_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/utils-lib.git"
	     :language "clojure_script"
	     :project-type "library"
     }
     {
	     :name "WebSocket library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "websocket-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/websocket_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/websocket-lib.git"
	     :language "clojurescript"
	     :project-type "library"
     }
     {
	     :name "Integrated development environment middleware"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "ide-middle"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure_script/projects/ide_middle"
	     :git-remote-link "git@github.com:VladimirMarkovic86/ide-middle.git"
	     :language "clojure_script"
	     :project-type "library"
     }
     {
	     :name "Request server library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "request-server-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/request_server_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/request-server-lib.git"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Language library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "language-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/language_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/language-lib.git"
	     :language "clojurescript"
	     :project-type "library"
     }
     {
	     :name "Sample middleware"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "sample-middle"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure_script/projects/sample_middle"
	     :git-remote-link "git@github.com:VladimirMarkovic86/sample-middle.git"
	     :language "clojure_script"
	     :project-type "library"
     }
     {
	     :name "Common server"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "common-server"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/common_server"
	     :git-remote-link "git@github.com:VladimirMarkovic86/common-server.git"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Optical character recognition middleware"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "ocr-middle"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure_script/projects/ocr_middle"
	     :git-remote-link "git@github.com:VladimirMarkovic86/ocr-middle.git"
	     :language "clojure_script"
	     :project-type "library"
     }
     {
	     :name "Common client"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "common-client"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/common_client"
	     :git-remote-link "git@github.com:VladimirMarkovic86/common-client.git"
	     :language "clojurescript"
	     :project-type "library"
     }
     {
	     :name "Common middle"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "common-middle"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure_script/projects/common_middle"
	     :git-remote-link "git@github.com:VladimirMarkovic86/common-middle.git"
	     :language "clojure_script"
	     :project-type "library"
     }
     {
	     :name "Personal organiser middleware"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "personal-organiser-middle"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure_script/projects/personal_organiser_middle"
	     :git-remote-link "git@github.com:VladimirMarkovic86/personal-organiser-middle.git"
	     :language "clojure_script"
	     :project-type "library"
     }
     {
	     :name "Database library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "db-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/db_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/db-lib.git"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Audit library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "audit-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/audit_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/audit-lib.git"
	     :language "clojure"
	     :project-type "library"
     }
     {
	     :name "Validator library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "validator-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojurescript/projects/validator_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/validator-lib.git"
	     :language "clojurescript"
	     :project-type "library"
     }
     {
	     :name "Personal organiser library"
	     :group-id "org.clojars.vladimirmarkovic86"
	     :artifact-id "personal-organiser-lib"
	     :version "0.1.0"
	     :absolute-path "/home/vladimir/workspace/clojure/projects/personal_organiser_lib"
	     :git-remote-link "git@github.com:VladimirMarkovic86/personal-organiser-lib.git"
	     :language "clojure"
	     :project-type "library"
     }
     ]))

