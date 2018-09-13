(defproject org.vladimir/ide-server "0.1.0"
  :description "Integrated development environment server"
  :url "http://gitlab:1610/VladimirMarkovic86/ide-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure	"1.9.0"]
                 [org.vladimir/server-lib "0.1.0"]
                 [org.vladimir/mongo-lib "0.1.0"]
                 [org.vladimir/utils-lib "0.1.0"]
                 [org.vladimir/ajax-lib "0.1.0"]
                 [org.vladimir/session-lib "0.1.0"]
                 [org.vladimir/ide-middle "0.1.0"]
                 [org.vladimir/language-lib "0.1.0"]
                 [org.vladimir/common-server "0.1.0"]
                 ]

  ; AOT - Compailation ahead of time
  :main ^:skip-aot ide-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :repl-options {:port 8604})

