(ns ide-server.task.entity-test
  (:require [clojure.test :refer :all]
            [ide-server.task.entity :refer :all]
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
    [{ :code 1044
       :english "Bug"
       :serbian "Грешка" }
     { :code 1045
       :english "New functionality"
       :serbian "Нова функционалност" }
     { :code 1046
       :english "Refactoring"
       :serbian "Рефакторисање" }
     { :code 1047
       :english "Low"
       :serbian "Низак" }
     { :code 1048
       :english "Medium"
       :serbian "Средњи" }
     { :code 1049
       :english "High"
       :serbian "Висок" }
     { :code 1050
       :english "Easy"
       :serbian "Лак" }
     { :code 1051
       :english "Medium"
       :serbian "Средње" }
     { :code 1052
       :english "Hard"
       :serbian "Тежак" }
     { :code 1053
       :english "Open"
       :serbian "Отворен" }
     { :code 1054
       :english "Development"
       :serbian "Развој" }
     { :code 1055
       :english "Deployed"
       :serbian "Отпремљен" }
     { :code 1056
       :english "Testing"
       :serbian "Тестирање" }
     { :code 1057
       :english "Rejected"
       :serbian "Одбијен" }
     { :code 1058
       :english "Done"
       :serbian "Готов" }
     ]))

(defn destroy-db
  "Destroy testing database"
  []
  (mon/mongodb-drop-database
    db-name)
  (mon/mongodb-disconnect))

(deftest test-format-code-field
  (testing "Test format code field"
    
    (let [raw-code nil
          selected-language nil
          result (format-code-field
                   raw-code
                   selected-language)]
      
      (is
        (nil?
          result)
       )
      
     )
    
    (let [raw-code 1
          selected-language nil
          result (format-code-field
                   raw-code
                   selected-language)]
      
      (is
        (= result
           1)
       )
      
     )
    
    (let [raw-code 123.123
          selected-language nil
          result (format-code-field
                   raw-code
                   selected-language)]
      
      (is
        (= result
           123)
       )
      
     )
    
   ))

(deftest test-format-type-field
  (testing "Test format type field"
    
    (create-db)
    
    (let [raw-type nil
          chosen-language nil
          result (format-type-field
                   raw-type
                   chosen-language)]
      
      (is
        (nil?
          result)
       )
      
     )
    
    (let [raw-type "unknown"
          chosen-language nil
          result (format-type-field
                   raw-type
                   chosen-language)]
      
      (is
        (= result
           "unknown")
       )
      
     )
    
    (let [raw-type "bug"
          chosen-language nil
          result (format-type-field
                   raw-type
                   chosen-language)]
      
      (is
        (= result
           "Bug")
       )
      
     )
    
    (let [raw-type "new_functionality"
          chosen-language "serbian"
          result (format-type-field
                   raw-type
                   chosen-language)]
      
      (is
        (= result
           "Нова функционалност")
       )
      
     )
    
    (let [raw-type "refactoring"
          chosen-language "serbian"
          result (format-type-field
                   raw-type
                   chosen-language)]
      
      (is
        (= result
           "Рефакторисање")
       )
      
     )
    
    (destroy-db)
    
   ))

(deftest test-format-priority-field
  (testing "Test format priority field"
    
    (create-db)
    
    (let [raw-priority nil
          chosen-language nil
          result (format-priority-field
                   raw-priority
                   chosen-language)]
      
      (is
        (nil?
          result)
       )
      
     )
    
    (let [raw-priority "unknown"
          chosen-language nil
          result (format-priority-field
                   raw-priority
                   chosen-language)]
      
      (is
        (= result
           "unknown")
       )
      
     )
    
    (let [raw-priority "low"
          chosen-language nil
          result (format-priority-field
                   raw-priority
                   chosen-language)]
      
      (is
        (= result
           "Low")
       )
      
     )
    
    (let [raw-priority "medium"
          chosen-language "serbian"
          result (format-priority-field
                   raw-priority
                   chosen-language)]
      
      (is
        (= result
           "Средњи")
       )
      
     )
    
    (let [raw-priority "high"
          chosen-language "serbian"
          result (format-priority-field
                   raw-priority
                   chosen-language)]
      
      (is
        (= result
           "Висок")
       )
      
     )
    
    (destroy-db)
    
   ))

(deftest test-format-difficulty-field
  (testing "Test format difficulty field"
    
    (create-db)
    
    (let [raw-difficulty nil
          chosen-language nil
          result (format-difficulty-field
                   raw-difficulty
                   chosen-language)]
      
      (is
        (nil?
          result)
       )
      
     )
    
    (let [raw-difficulty "unknown"
          chosen-language nil
          result (format-difficulty-field
                   raw-difficulty
                   chosen-language)]
      
      (is
        (= result
           "unknown")
       )
      
     )
    
    (let [raw-difficulty "easy"
          chosen-language nil
          result (format-difficulty-field
                   raw-difficulty
                   chosen-language)]
      
      (is
        (= result
           "Easy")
       )
      
     )
    
    (let [raw-difficulty "medium"
          chosen-language "serbian"
          result (format-difficulty-field
                   raw-difficulty
                   chosen-language)]
      
      (is
        (= result
           "Средње")
       )
      
     )
    
    (let [raw-difficulty "hard"
          chosen-language "serbian"
          result (format-difficulty-field
                   raw-difficulty
                   chosen-language)]
      
      (is
        (= result
           "Тежак")
       )
      
     )
    
    (destroy-db)
    
   ))

(deftest test-format-status-field
  (testing "Test format status field"
    
    (create-db)
    
    (let [raw-status nil
          chosen-language nil
          result (format-status-field
                   raw-status
                   chosen-language)]
      
      (is
        (nil?
          result)
       )
      
     )
    
    (let [raw-status "unknown"
          chosen-language nil
          result (format-status-field
                   raw-status
                   chosen-language)]
      
      (is
        (= result
           "unknown")
       )
      
     )
    
    (let [raw-status "open"
          chosen-language nil
          result (format-status-field
                   raw-status
                   chosen-language)]
      
      (is
        (= result
           "Open")
       )
      
     )
    
    (let [raw-status "development"
          chosen-language nil
          result (format-status-field
                   raw-status
                   chosen-language)]
      
      (is
        (= result
           "Development")
       )
      
     )
    
    (let [raw-status "deployed"
          chosen-language nil
          result (format-status-field
                   raw-status
                   chosen-language)]
      
      (is
        (= result
           "Deployed")
       )
      
     )
    
    (let [raw-status "testing"
          chosen-language "serbian"
          result (format-status-field
                   raw-status
                   chosen-language)]
      
      (is
        (= result
           "Тестирање")
       )
      
     )
    
    (let [raw-status "rejected"
          chosen-language "serbian"
          result (format-status-field
                   raw-status
                   chosen-language)]
      
      (is
        (= result
           "Одбијен")
       )
      
     )
    
    (let [raw-status "done"
          chosen-language "serbian"
          result (format-status-field
                   raw-status
                   chosen-language)]
      
      (is
        (= result
           "Готов")
       )
      
     )
    
    (destroy-db)
    
   ))

