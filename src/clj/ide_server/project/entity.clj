(ns ide-server.project.entity
  (:require [language-lib.core :refer [get-label]]
            [ide-middle.project.entity :as impe]
            [common-server.preferences :as prf]))

(defn format-group-id-field
  "Formats group-id field in \\nolinkurl{group-id} format"
  [raw-group-id
   chosen-language]
  (when (and raw-group-id
             (string?
               raw-group-id))
    (str
      "nolinkurlopenbraces"
      raw-group-id
      "closedbraces"))
 )

(defn format-language-field
  "Formats language field"
  [raw-language
   selected-language]
  (when (and raw-language
             (string?
               raw-language))
    (let [language-a (atom raw-language)]
      (when (= raw-language
               impe/clojure)
        (reset!
          language-a
          (get-label
            1071
            selected-language))
       )
      (when (= raw-language
               impe/clojure-script)
        (reset!
          language-a
          (get-label
            1072
            selected-language))
       )
      (when (= raw-language
               impe/clojurescript)
        (reset!
          language-a
          (get-label
            1073
            selected-language))
       )
      @language-a))
 )

(defn format-project-type-field
  "Formats project type field"
  [raw-project-type
   selected-language]
  (when (and raw-project-type
             (string?
               raw-project-type))
    (let [language-a (atom raw-project-type)]
      (when (= raw-project-type
               impe/application)
        (reset!
          language-a
          (get-label
            1074
            selected-language))
       )
      (when (= raw-project-type
               impe/library)
        (reset!
          language-a
          (get-label
            1075
            selected-language))
       )
      @language-a))
 )

(defn reports
  "Returns reports projection"
  [request
   & [chosen-language]]
  (prf/set-preferences
    request)
  {:entity-label (get-label
                   1001
                   chosen-language)
   :projection [;:name
                :group-id
                :artifact-id
                ;:version
                ;:absolute-path
                ;:git-remote-link
                :language
                :project-type
                ]
   :qsort {:artifact-id 1}
   :rows (int
           (impe/calculate-rows))
   :table-rows (int
                 @impe/table-rows-a)
   :card-columns (int
                   @impe/card-columns-a)
   :labels {:name (get-label
                    1003
                    chosen-language)
            :group-id (get-label
                        1004
                        chosen-language)
            :artifact-id (get-label
                           1005
                           chosen-language)
            :version (get-label
                       1006
                       chosen-language)
            :absolute-path (get-label
                             1007
                             chosen-language)
            :git-remote-link (get-label
                               1008
                               chosen-language)
            :language (get-label
                        1009
                        chosen-language)
            :project-type (get-label
                            1010
                            chosen-language)
            }
   :columns {:name {:width "70"
                    :header-background-color "lightblue"
                    :header-text-color "white"}
             :group-id {:width "45"
                        :header-background-color "lightblue"
                        :header-text-color "white"
                        :data-format-fn format-group-id-field}
             :artifact-id {:width "35"
                           :header-background-color "lightblue"
                           :header-text-color "white"
                           :column-alignment "C"}
             :version {:width "15"
                       :header-background-color "lightblue"
                       :header-text-color "white"
                       :column-alignment "C"}
             :absolute-path {:width "15"
                             :header-background-color "lightblue"
                             :header-text-color "white"}
             :git-remote-link {:width "15"
                               :header-background-color "lightblue"
                               :header-text-color "white"
                               :column-alignment "C"}
             :language {:width "35"
                        :header-background-color "lightblue"
                        :header-text-color "white"
                        :data-format-fn format-language-field
                        :column-alignment "C"}
             :project-type {:width "25"
                            :header-background-color "lightblue"
                            :header-text-color "white"
                            :data-format-fn format-project-type-field
                            :column-alignment "C"}
             }
   })

