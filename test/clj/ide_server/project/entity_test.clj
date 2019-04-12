(ns ide-server.project.entity-test
  (:require [clojure.test :refer :all]
            [ide-server.project.entity :refer :all]
            [mongo-lib.core :as mon]))

(def db-uri
     (or (System/getenv "MONGODB_URI")
         (System/getenv "PROD_MONGODB")
         "mongodb://admin:passw0rd@127.0.0.1:27017/admin"))

(def db-name
     "test-db")

(defn create-db
  "Create database for testing"
  []
  (mon/mongodb-connect
    db-uri
    db-name)
  (mon/mongodb-insert-many
    "language"
    [{ :code 1071
       :english "Clojure"
       :serbian "Clojure" }
     { :code 1072
       :english "Clojure/Script"
       :serbian "Clojure/Script" }
     { :code 1073
       :english "ClojureScript"
       :serbian "ClojureScript" }
     { :code 1074
       :english "Application"
       :serbian "Апликација" }
     { :code 1075
       :english "Library"
       :serbian "Библиотека" }
     ]))

(defn destroy-db
  "Destroy testing database"
  []
  (mon/mongodb-drop-database
    db-name)
  (mon/mongodb-disconnect))

(deftest test-format-language-field
  (testing "Test format language field"
    
    (create-db)
    
    (let [raw-language nil
          chosen-language nil
          result (format-language-field
                   raw-language
                   chosen-language)]
      
      (is
        (nil?
          result)
       )
      
     )
    
    (let [raw-language "unknown"
          chosen-language nil
          result (format-language-field
                   raw-language
                   chosen-language)]
      
      (is
        (= result
           "unknown")
       )
      
     )
    
    (let [raw-language "clojure"
          chosen-language nil
          result (format-language-field
                   raw-language
                   chosen-language)]
      
      (is
        (= result
           "Clojure")
       )
      
     )
    
    (let [raw-language "clojure_script"
          chosen-language "serbian"
          result (format-language-field
                   raw-language
                   chosen-language)]
      
      (is
        (= result
           "Clojure/Script")
       )
      
     )
    
    (let [raw-language "clojurescript"
          chosen-language "serbian"
          result (format-language-field
                   raw-language
                   chosen-language)]
      
      (is
        (= result
           "ClojureScript")
       )
      
     )
    
    (destroy-db)
    
   ))

(deftest test-format-project-type-field
  (testing "Test format project type field"
    
    (create-db)
    
    (let [raw-project-type nil
          chosen-language nil
          result (format-project-type-field
                   raw-project-type
                   chosen-language)]
      
      (is
        (nil?
          result)
       )
      
     )
    
    (let [raw-project-type "unknown"
          chosen-language nil
          result (format-project-type-field
                   raw-project-type
                   chosen-language)]
      
      (is
        (= result
           "unknown")
       )
      
     )
    
    (let [raw-project-type "application"
          chosen-language nil
          result (format-project-type-field
                   raw-project-type
                   chosen-language)]
      
      (is
        (= result
           "Application")
       )
      
     )
    
    (let [raw-project-type "library"
          chosen-language "serbian"
          result (format-project-type-field
                   raw-project-type
                   chosen-language)]
      
      (is
        (= result
           "Библиотека")
       )
      
     )
    
    (destroy-db)
    
   ))

