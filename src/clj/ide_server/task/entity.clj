(ns ide-server.task.entity
  (:require [language-lib.core :refer [get-label]]
            [ide-middle.task.entity :as imte]))

(defn format-code-field
  "Formats code field"
  [raw-code
   selected-language]
  (if (and raw-code
           (number?
             raw-code))
    (int
      raw-code)
    raw-code))

(defn format-type-field
  "Formats type field"
  [raw-type
   selected-language]
  (when (and raw-type
             (string?
               raw-type))
    (let [type-a (atom raw-type)]
      (when (= raw-type
               imte/type-bug)
        (reset!
          type-a
          (get-label
            1044
            selected-language))
       )
      (when (= raw-type
               imte/type-new-functionality)
        (reset!
          type-a
          (get-label
            1045
            selected-language))
       )
      (when (= raw-type
               imte/type-refactoring)
        (reset!
          type-a
          (get-label
            1046
            selected-language))
       )
      @type-a))
 )

(defn format-priority-field
  "Formats priority field"
  [raw-priority
   selected-language]
  (when (and raw-priority
             (string?
               raw-priority))
    (let [priority-a (atom raw-priority)]
      (when (= raw-priority
               imte/priority-low)
        (reset!
          priority-a
          (get-label
            1047
            selected-language))
       )
      (when (= raw-priority
               imte/priority-medium)
        (reset!
          priority-a
          (get-label
            1048
            selected-language))
       )
      (when (= raw-priority
               imte/priority-high)
        (reset!
          priority-a
          (get-label
            1049
            selected-language))
       )
      @priority-a))
 )

(defn format-difficulty-field
  "Formats difficulty field"
  [raw-difficulty
   selected-language]
  (when (and raw-difficulty
             (string?
               raw-difficulty))
    (let [difficulty-a (atom raw-difficulty)]
      (when (= raw-difficulty
               imte/difficulty-easy)
        (reset!
          difficulty-a
          (get-label
            1050
            selected-language))
       )
      (when (= raw-difficulty
               imte/difficulty-medium)
        (reset!
          difficulty-a
          (get-label
            1051
            selected-language))
       )
      (when (= raw-difficulty
               imte/difficulty-hard)
        (reset!
          difficulty-a
          (get-label
            1052
            selected-language))
       )
      @difficulty-a))
 )

(defn format-status-field
  "Formats status field"
  [raw-status
   selected-language]
  (when (and raw-status
             (string?
               raw-status))
    (let [status-a (atom raw-status)]
      (when (= raw-status
               imte/status-open)
        (reset!
          status-a
          (get-label
            1053
            selected-language))
       )
      (when (= raw-status
               imte/status-development)
        (reset!
          status-a
          (get-label
            1054
            selected-language))
       )
      (when (= raw-status
               imte/status-deployed)
        (reset!
          status-a
          (get-label
            1055
            selected-language))
       )
      (when (= raw-status
               imte/status-testing)
        (reset!
          status-a
          (get-label
            1056
            selected-language))
       )
      (when (= raw-status
               imte/status-rejected)
        (reset!
          status-a
          (get-label
            1057
            selected-language))
       )
      (when (= raw-status
               imte/status-done)
        (reset!
          status-a
          (get-label
            1058
            selected-language))
       )
      @status-a))
 )

(defn reports
  "Returns reports projection"
  [& [chosen-language]]
  {:entity-label (get-label
                   1043
                   chosen-language)
   :projection [:code
                :name
                ;:description
                :type
                :priority
                ;:difficulty
                :status
                ;:estimated-time
                ;:taken-time
                ]
   :qsort {:code 1}
   :rows imte/rows
   :labels {:code (get-label
                    1035
                    chosen-language)
            :name (get-label
                    1003
                    chosen-language)
            :description (get-label
                           1036
                           chosen-language)
            :type (get-label
                    1037
                    chosen-language)
            :priority (get-label
                        1038
                        chosen-language)
            :difficulty (get-label
                          1039
                          chosen-language)
            :status (get-label
                      1040
                      chosen-language)
            :estimated-time (get-label
                              1041
                              chosen-language)
            :taken-time (get-label
                          1042
                          chosen-language)
            }
   :columns {:code {:width "20"
                    :header-background-color "lightblue"
                    :header-text-color "white"
                    :data-format-fn format-code-field
                    :column-alignment "C"}
             :name {:width "44"
                    :header-background-color "lightblue"
                    :header-text-color "white"}
             :description {:width "35"
                           :header-background-color "lightblue"
                           :header-text-color "white"
                           :column-alignment "C"}
             :type {:width "28"
                    :header-background-color "lightblue"
                    :header-text-color "white"
                    :data-format-fn format-type-field
                    :column-alignment "C"}
             :priority {:width "20"
                        :header-background-color "lightblue"
                        :header-text-color "white"
                        :data-format-fn format-priority-field
                        :column-alignment "C"}
             :difficulty {:width "15"
                          :header-background-color "lightblue"
                          :header-text-color "white"
                          :data-format-fn format-difficulty-field
                          :column-alignment "C"}
             :status {:width "20"
                      :header-background-color "lightblue"
                      :header-text-color "white"
                      :data-format-fn format-status-field
                      :column-alignment "C"}
             :estimated-time {:width "35"
                              :header-background-color "lightblue"
                              :header-text-color "white"
                              :column-alignment "C"}
             :taken-time {:width "35"
                          :header-background-color "lightblue"
                          :header-text-color "white"
                          :column-alignment "C"}
             }
   })

