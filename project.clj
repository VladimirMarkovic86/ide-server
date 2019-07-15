(defproject org.clojars.vladimirmarkovic86/ide-server "0.2.1"
  :description "Integrated development environment server"
  :url "http://github.com/VladimirMarkovic86/ide-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojars.vladimirmarkovic86/server-lib "0.3.33"]
                 [org.clojars.vladimirmarkovic86/mongo-lib "0.2.10"]
                 [org.clojars.vladimirmarkovic86/utils-lib "0.4.11"]
                 [org.clojars.vladimirmarkovic86/ajax-lib "0.1.12"]
                 [org.clojars.vladimirmarkovic86/session-lib "0.2.26"]
                 [org.clojars.vladimirmarkovic86/ide-middle "0.2.14"]
                 [org.clojars.vladimirmarkovic86/common-middle "0.2.9"]
                 [org.clojars.vladimirmarkovic86/common-server "0.3.42"]
                 [org.clojars.vladimirmarkovic86/audit-lib "0.1.34"]
                 [org.clojars.vladimirmarkovic86/pdflatex-lib "0.1.6"]
                 ]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  
  :main ^:skip-aot ide-server.core
    
  :uberjar-name "ide-server-standalone.jar"
  :profiles {:uberjar {:aot :all}}
  :repl-options {:port 8604})

