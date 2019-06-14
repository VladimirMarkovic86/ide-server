(ns ide-server.preferences
  (:require [ide-middle.project.entity :as impe]
            [ide-middle.task.entity :as imte]))

(defn set-specific-preferences-fn
  "Sets preferences on server side"
  [specific-map]
  (let [{{{table-rows-p :table-rows
           card-columns-p :card-columns} :project-entity
          {table-rows-t :table-rows
           card-columns-t :card-columns} :task-entity} :display} specific-map]
    (reset!
      impe/table-rows-a
      (or table-rows-p
          10))
    (reset!
      impe/card-columns-a
      (or card-columns-p
          0))
    (reset!
      imte/table-rows-a
      (or table-rows-t
          10))
    (reset!
      imte/card-columns-a
      (or card-columns-t
          0))
   ))

