(ns ide-server.core
  (:gen-class)
  (:require [session-lib.core :as ssn]
            [server-lib.core :as srvr]
            [utils-lib.core :as utils :refer [parse-body]]
            [mongo-lib.core :as mon]
            [ajax-lib.http.entity-header :as eh]
            [ajax-lib.http.response-header :as rsh]
            [ajax-lib.http.mime-type :as mt]
            [ajax-lib.http.status-code :as stc]
            [ide-middle.functionalities :as imfns]
            [ide-middle.request-urls :as irurls]
            [ide-middle.project.entity :as pem]
            [common-server.core :as rt]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as cstring])
  (:import [java.io FileNotFoundException]))

(def db-name
     "ide-db")

(def stopped
     "stopped")

(def running
     "running")

(defn execute-shell-command
  "Execute shell command from sh file at it's file path"
  [command]
  (let [final-command (atom 
                        ["/home/vladimir/workspace/clojure/projects/ide_server/sh"])
        result (atom nil)]
    (when (string? command)
      (swap!
        final-command
        conj
        command)
      (reset!
        result
        (apply
          sh
          @final-command))
     )
    (when (vector? command)
      (doseq [cmd command]
        (swap!
          final-command
          conj
          cmd))
      (reset!
        result
        (apply
          sh
          @final-command))
     )
    @result))

(defn execute-shell-command-fn
  "Execute shell command function with response"
  [request-body]
  (let [command (:command request-body)
        output (execute-shell-command
                 command)]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :data output})})
 )

(defn read-file
  "Read file if supported and return it in response as entity body"
  [request-body]
  (try
    (let [file-path (:file-path request-body)
          operation (:operation request-body)
          body (atom nil)
          headers (atom nil)]
      (when (= operation
               "read")
        (reset!
          headers
          {(eh/content-type) (mt/text-plain)})
        (reset!
          body
          (slurp
            (clojure.java.io/file
              file-path))
         ))
      (when (contains?
              #{"image"
                "video"}
              operation)
        (let [f (java.io.File. file-path)
              ary (byte-array (.length f))
              is (java.io.FileInputStream. f)
              extension-start (cstring/last-index-of
                                file-path
                                ".")
              extension (.substring
                          file-path
                          (inc extension-start)
                          (count file-path))]
          (.read is ary)
          (.close is)
          (reset!
            headers
            {(eh/content-type) (str
                                 operation
                                 "/"
                                 extension)})
          (reset!
            body
            ary))
       )
      {:status (stc/ok)
       :headers @headers
       :body @body})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str {:status "error"
                   :error-message (.getMessage e)})}
     ))
 )

(defn list-documents-fn
  "List documents from dir-path"
  [request-body]
  (let [dir-path (:dir-path request-body)
        output (execute-shell-command
                 (str
                   "ls -al " dir-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :data output})})
 )

(defn mkdir-fn
  "Make directory in dir-path"
  [request-body]
  (let [dir-path (:dir-path request-body)
        output (execute-shell-command
                 (str
                   "mkdir " dir-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :data output})})
 )

(defn mkfile-fn
  "Make file in file-path"
  [request-body]
  (let [file-path (:file-path request-body)
        output (execute-shell-command
                 (str
                   "touch " file-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :data output})})
 )

(defn move-document-fn
  "Move document in dest-path"
  [request-body]
  (let [doc-path (:doc-path request-body)
        dest-path (:dest-path request-body)
        output (execute-shell-command
                 (str
                   "mv " doc-path " " dest-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :data output})})
 )

(defn copy-document-fn
  "Copy document in dest-path"
  [request-body]
  (let [doc-path (:doc-path request-body)
        dest-path (:dest-path request-body)
        output (execute-shell-command
                 (str
                   "cp -r " doc-path " " dest-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :data output})})
 )

(defn delete-document-fn
  "Delete document doc-path"
  [request-body]
  (let [doc-path (:doc-path request-body)
        output (execute-shell-command
                 (str
                   "rm -rf " doc-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :data output})})
 )

(defn project-name
  "Format project name"
  [group-id
   artifact-id
   version]
  (str
    group-id
    "/"
    artifact-id
    "-"
    version))

(defn build-project
  "Build project fetched by _id"
  [request-body]
  (let [entity-id (:entity-id request-body)
        entity-type (:entity-type request-body)
        {group-id :group-id
         artifact-id :artifact-id
         version :version
         absolute-path :absolute-path
         language :language
         project-type :project-type} (mon/mongodb-find-by-id
                                       entity-type
                                       entity-id)
        output (atom nil)]
    (when (= project-type
             pem/library)
      (reset!
        output
        (execute-shell-command
          [(str
             "cd " absolute-path)
           "lein install"]))
     )
    (when (and (= project-type
                  pem/application)
               (contains?
                 #{pem/clojure-script
                   pem/clojurescript}
                 language))
      (reset!
        output
        (execute-shell-command
          [(str
             "cd " absolute-path)
           "lein cljsbuild once dev"]))
     )
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :heading (str
                            "Build project "
                            (project-name
                              group-id
                              artifact-id
                              version))
                 :data @output})})
 )

(defn build-project-dependencies
  "Build project dependencies by _id"
  [request-body]
  (let [entity-id (:entity-id request-body)
        entity-type (:entity-type request-body)
        {m-group-id :group-id
         m-artifact-id :artifact-id
         m-version :version
         m-absolute-path :absolute-path
         m-language :language
         m-project-type :project-type} (mon/mongodb-find-by-id
                                         entity-type
                                         entity-id)
        project-clj (slurp
                      (clojure.java.io/file
                        (str
                          m-absolute-path
                          "/project.clj"))
                     )
        projects (mon/mongodb-find
                   entity-type)
        apply-conj-fn (fn [atom-value
                           param]
                        (apply
                          conj
                          atom-value
                          param))
        build-projects (atom (sorted-set))
        build-command (atom [])
        output (atom nil)]
    (doseq [{group-id :group-id
             artifact-id :artifact-id
             version :version
             absolute-path :absolute-path
             language :language
             project-type :project-type} projects]
      (when-let [i (cstring/index-of
                     project-clj
                     (str
                       group-id
                       "/"
                       artifact-id))]
        (when (not
                (and (= group-id
                        m-group-id)
                     (= artifact-id
                        m-artifact-id))
               )
          (swap!
            build-projects
            conj
            [i
             absolute-path
             group-id
             artifact-id
             version]))
       ))
    (doseq [[_
             absolute-path] @build-projects]
      (swap!
        build-command
        apply-conj-fn
        [(str
           "cd " absolute-path)
         "lein install"]))
    (when (= m-project-type
             pem/library)
      (swap!
        build-command
        apply-conj-fn
        [(str
           "cd " m-absolute-path)
         "lein install"]))
    (when (and (= m-project-type
                  pem/application)
               (contains?
                 #{pem/clojure-script
                   pem/clojurescript}
                 m-language))
      (swap!
        build-command
        apply-conj-fn
        [(str
           "cd " m-absolute-path)
         "lein cljsbuild once dev"]))
    (reset!
      output
      (execute-shell-command
        @build-command))
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :heading (str
                            "Build dependencies of "
                            m-group-id
                            "/"
                            m-artifact-id
                            "-"
                            m-version)
                 :data @output})})
 )

(defn clean-project
  "Clean project by _id"
  [request-body]
  (let [entity-id (:entity-id request-body)
        entity-type (:entity-type request-body)
        {group-id :group-id
         artifact-id :artifact-id
         version :version
         absolute-path :absolute-path
         language :language
         project-type :project-type} (mon/mongodb-find-by-id
                                       entity-type
                                       entity-id)
        output (execute-shell-command
                 [(str
                    "cd " absolute-path)
                  "lein clean"
                  "rm -rf resources/public/js/ resources/public/jsprod/"])]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :heading (str
                         "Clean "
                         (project-name
                           group-id
                           artifact-id
                           version))
              :data output})})
 )

(defn server-status-fn
  "Check server status of application project"
  [request-body]
  (let [status (atom "no status")]
    (try
      (let [entity-id (:entity-id request-body)
            entity-type (:entity-type request-body)
            {group-id :group-id
             artifact-id :artifact-id
             version :version
             absolute-path :absolute-path
             language :language
             project-type :project-type} (mon/mongodb-find-by-id
                                           entity-type
                                           entity-id)]
        (when (and (contains?
                     #{pem/clojure
                       pem/clojurescript}
                     language)
                   (= project-type
                      pem/application))
          (let [pid (slurp
                      (io/file
                        (str
                          absolute-path
                          "/pid.file"))
                     )
                pid (cstring/replace
                      pid
                      "\n"
                      "")
                out (execute-shell-command
                      (str
                        "ps p " pid " | grep " pid))
                out (:out out)]
            (if (empty? out)
              (reset!
                status
                stopped)
              (reset!
                status
                running))
           ))
       )
      (catch FileNotFoundException e
        (reset!
          status
          stopped)
        (println (.getMessage e))
       )
      (catch Exception e
        (reset!
          status
          (.getMessage e))
        (println (.getMessage e))
       ))
      @status))

(defn start-server-fn
  "Start server of application project"
  [request-body]
  (let [status (atom
                 (server-status-fn
                   request-body))]
    (try
      (when (= @status
               stopped)
        (let [entity-id (:entity-id request-body)
              entity-type (:entity-type request-body)
              {group-id :group-id
               artifact-id :artifact-id
               version :version
               absolute-path :absolute-path
               language :language
               project-type :project-type} (mon/mongodb-find-by-id
                                             entity-type
                                             entity-id)]
          (when (and (contains?
                       #{pem/clojure
                         pem/clojurescript}
                       language)
                     (= project-type
                        pem/application))
            (execute-shell-command
              [(str
                 "cd " absolute-path)
               "lein trampoline run &> server.log & echo $! >pid.file &"])
            (reset!
              status
              (server-status-fn
                request-body))
           ))
       )
     (catch Exception e
       (reset!
         status
         (.getMessage e))
       (println (.getMessage e))
      ))
     @status))

(defn stop-server-fn
  "Stop server of application project"
  [request-body]
  (let [status (atom
                 (server-status-fn
                   request-body))]
    (try
      (when (= @status
               running)
        (let [entity-id (:entity-id request-body)
              entity-type (:entity-type request-body)
              {group-id :group-id
               artifact-id :artifact-id
               version :version
               absolute-path :absolute-path
               language :language
               project-type :project-type} (mon/mongodb-find-by-id
                                             entity-type
                                             entity-id)]
          (when (and (contains?
                       #{pem/clojure
                         pem/clojurescript}
                       language)
                     (= project-type
                        pem/application))
            (let [pid (slurp
                        (io/file
                          (str
                            absolute-path
                            "/pid.file"))
                       )
                  pid (cstring/replace
                        pid
                        "\n"
                        "")
                  out (execute-shell-command
                        (str
                          "kill -9 " pid))]
              (reset!
                status
                (server-status-fn
                  request-body))
              (when (= @status
                       stopped)
                (execute-shell-command
                  (str
                    "rm -rf " absolute-path "/pid.file"))
               ))
           ))
       )
      (catch Exception e
        (reset!
          status
          (.getMessage e))
        (println (.getMessage e))
       ))
      @status))

(defn restart-server-fn
  "Restart server of application project"
  [request-body]
  (let [status (atom
                 (stop-server-fn
                   request-body))]
    (reset!
      status
      (start-server-fn
        request-body))
    @status))

(defn run-project
  "Interact with project wit start, stop, status and restart commands"
  [request-body]
  (let [entity-id (:entity-id request-body)
        entity-type (:entity-type request-body)
        {group-id :group-id
         artifact-id :artifact-id
         version :version} (mon/mongodb-find-by-id
                             entity-type
                             entity-id)
        action (:action request-body)
        status (atom "")]
    (when (= action
             pem/start)
      (reset!
        status
        (start-server-fn
          request-body))
     )
    (when (= action
             pem/stop)
      (reset!
        status
        (stop-server-fn
          request-body))
     )
    (when (= action
             pem/restart)
      (reset!
        status
        (restart-server-fn
          request-body))
     )
    (when (= action
             pem/status)
      (reset!
        status
        (server-status-fn
          request-body))
     )
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :heading (str
                         "Run "
                         (project-name
                           group-id
                           artifact-id
                           version))
              :data {:out @status}})})
 )

(defn git-project
  "Interact with project with git commands"
  [request-body]
  (let [entity-id (:entity-id request-body)
        entity-type (:entity-type request-body)
        action (:action request-body)
        {group-id :group-id
         artifact-id :artifact-id
         version :version
         absolute-path :absolute-path
         language :language
         project-type :project-type} (mon/mongodb-find-by-id
                                       entity-type
                                       entity-id)
        output (execute-shell-command
                 [(str
                    "cd " absolute-path)
                  (str
                    "git " action )])]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :heading (str
                         "Git "
                         (project-name
                           group-id
                           artifact-id
                           version))
              :data output})})
 )

(defn git-status
  "Check git status of project"
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git status -s"])]
    output))

(defn git-init
  "Initialize git project if not initialized yet"
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git init"])]
    output))

(defn git-remote-add
  "Add remote git repository with ssh or https url"
  [root-dir
   git-remote-repo-link]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git remote add origin " git-remote-repo-link)])]
    output))

(defn git-remote-remove
  "Remove remote repository url"
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git remote remove origin"])]
    output))

(defn git-add
  "Git add new file or file with changes"
  [root-dir
   file-path]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git add " file-path)])]
    output))

(defn git-rm
  "Git remove file"
  [root-dir
   file-path]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git rm " file-path)])]
    output))

(defn git-reset
  "Git reset, exclude particular file from commit if it was added
   doesn't work for \"git rm\""
  [root-dir
   file-path]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git reset " file-path " -q")])]
    output))

(defn git-commit
  "Git commit changes"
  [root-dir
   commit-message]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git commit -m '" commit-message "'")])]
    output))

(defn git-push
  "Git push commits to remote repository"
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git push"])]
    output))

(defn git-unpushed-commits
  "Check for unpushed commits"
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git log origin/master..HEAD --oneline"])]
    output))

(defn git-diff
  "Git diff of project"
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git diff"])]
    output))

(defn git-project
  "Interact with project with git commands"
  [request-body]
  (let [entity-id (:entity-id request-body)
        entity-type (:entity-type request-body)
        action (:action request-body)
        new-git-remote-link (:new-git-remote-link request-body)
        file-path (:file-path request-body)
        commit-message (:commit-message request-body)
        entity (mon/mongodb-find-by-id
                 entity-type
                 entity-id)
        {group-id :group-id
         artifact-id :artifact-id
         version :version
         absolute-path :absolute-path
         git-remote-link :git-remote-link
         language :language
         project-type :project-type} entity
        project-diff (atom "")
        unpushed-commits (atom "")
        output (atom "")]
    (when (= action
             pem/git-status)
      (reset!
        output
        (git-status
          absolute-path))
     )
    (when (= action
             pem/git-init)
      (reset!
        output
        (git-init
          absolute-path))
      (swap!
        output
        str
        (git-remote-add
          absolute-path
          new-git-remote-link))
      (mon/mongodb-update-by-id
        entity-type
        entity-id
        {:git-remote-link new-git-remote-link})
     )
    (when (= action
             pem/git-remote-change)
      (reset!
        output
        (git-remote-remove
          absolute-path))
      (swap!
        output
        (git-remote-add
          absolute-path
          new-git-remote-link))
      (mon/mongodb-update-by-id
        entity-type
        entity-id
        {:git-remote-link new-git-remote-link})
     )
    (when (= action
             pem/git-add)
      (reset!
        output
        (git-add
          absolute-path
          file-path))
     )
    (when (= action
             pem/git-rm)
      (reset!
        output
        (git-rm
          absolute-path
          file-path))
     )
    (when (= action
             pem/git-reset)
      (reset!
        output
        (git-reset
          absolute-path
          file-path))
     )
    (when (= action
             pem/git-commit)
      (reset!
        output
        (git-commit
          absolute-path
          commit-message))
     )
    (when (= action
             pem/git-commit-push)
      (reset!
        output
        (git-commit
          absolute-path
          commit-message))
      (swap!
        output
        str
        (git-push
          absolute-path))
     )
    (when (= action
             pem/git-push)
      (reset!
        output
        (git-push
          absolute-path))
     )
    (reset!
      unpushed-commits
      (git-unpushed-commits
        absolute-path))
    (reset!
      project-diff
      (git-diff
        absolute-path))
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :git-remote-url (or new-git-remote-link
                                  git-remote-link)
              :unpushed-commits @unpushed-commits
              :project-diff @project-diff
              :data @output})})
 )

(defn save-file-changes
  "Save file changes"
  [request-body]
  (try
    (let [file-path (:file-path request-body)
          file-content (:file-content request-body)
          f (java.io.File.
              file-path)
          ary (.getBytes
                file-content
                "UTF-8")
          os (java.io.FileOutputStream.
               f)]
      (.write
        os
        ary)
      (.close
        os)
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str {:status "success"})})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str {:status "error"
                   :error-message (.getMessage e)})}
     ))
 )

(defn response-routing-fn
  "Custom routing function"
  [request]
  (let [{request-uri :request-uri
         request-method :request-method} request]
    (cond
      (= request-method
         "POST")
        (cond
          (= request-uri
             irurls/read-file-url)
            (read-file
              (parse-body
                request))
          (= request-uri
             irurls/execute-shell-command-url)
            (execute-shell-command-fn
              (parse-body
                request))
          (= request-uri
             irurls/list-documents-url)
            (list-documents-fn
              (parse-body
                request))
          (= request-uri
             irurls/new-folder-url)
            (mkdir-fn
              (parse-body
                request))
          (= request-uri
             irurls/new-file-url)
            (mkfile-fn
              (parse-body
                request))
          (= request-uri
             irurls/move-document-url)
            (move-document-fn
              (parse-body
                request))
          (= request-uri
             irurls/copy-document-url)
            (copy-document-fn
              (parse-body
                request))
          (= request-uri
             irurls/delete-document-url)
            (delete-document-fn
              (parse-body
                request))
          (= request-uri
             irurls/build-project-url)
            (build-project
              (parse-body
                request))
          (= request-uri
             irurls/build-project-dependencies-url)
            (build-project-dependencies
              (parse-body
                request))
          (= request-uri
             irurls/clean-project-url)
            (clean-project
              (parse-body
                request))
          (= request-uri
             irurls/run-project-url)
            (run-project
              (parse-body
                request))
          (= request-uri
             irurls/git-project-url)
            (git-project
              (parse-body
                request))
          (= request-uri
             irurls/git-status-url)
            (git-status
              (parse-body
                request))
          (= request-uri
             irurls/save-file-changes-url)
            (save-file-changes
              (parse-body
                request))
          :else
            nil)
      :else
        nil))
 )

(defn allow-action-routing-fn
  "Check if action is allowed function"
  [request]
  (let [allowed-functionalities (rt/get-allowed-actions
                                  request)
        {request-uri :request-uri
         request-method :request-method} request]
    (cond
      (= request-method
         "POST")
        (cond
          (= request-uri
             irurls/read-file-url)
            (contains?
              allowed-functionalities
              imfns/read-file)
          (= request-uri
             irurls/execute-shell-command-url)
            (contains?
              allowed-functionalities
              imfns/execute-shell-command)
          (= request-uri
             irurls/list-documents-url)
            (contains?
              allowed-functionalities
              imfns/list-documents)
          (= request-uri
             irurls/new-folder-url)
            (contains?
              allowed-functionalities
              imfns/new-folder)
          (= request-uri
             irurls/new-file-url)
            (contains?
              allowed-functionalities
              imfns/new-file)
          (= request-uri
             irurls/move-document-url)
            (contains?
              allowed-functionalities
              imfns/move-document)
          (= request-uri
             irurls/copy-document-url)
            (contains?
              allowed-functionalities
              imfns/copy-document)
          (= request-uri
             irurls/delete-document-url)
            (contains?
              allowed-functionalities
              imfns/delete-document)
          (= request-uri
             irurls/build-project-url)
            (contains?
              allowed-functionalities
              imfns/build-project)
          (= request-uri
             irurls/build-project-dependencies-url)
            (contains?
              allowed-functionalities
              imfns/build-project-dependencies)
          (= request-uri
             irurls/clean-project-url)
            (contains?
              allowed-functionalities
              imfns/clean-project)
          (= request-uri
             irurls/run-project-url)
            (contains?
              allowed-functionalities
              imfns/run-project)
          (= request-uri
             irurls/git-project-url)
            (contains?
              allowed-functionalities
              imfns/git-project)
          (= request-uri
             irurls/git-status-url)
            (contains?
              allowed-functionalities
              imfns/git-status)
          (= request-uri
             irurls/save-file-changes-url)
            (contains?
              allowed-functionalities
              imfns/save-file-changes)
          :else
            false)
      :else
        false))
 )

(defn routing
  "Routing function"
  [request]
  (rt/routing
    request
    (response-routing-fn
      request)
    (allow-action-routing-fn
      request))
 )

(defn start-server
  "Start server"
  []
  (try
    (srvr/start-server
      routing
      {(rsh/access-control-allow-origin) #{"https://ide:8455"
                                           "https://ide:1614"
                                           "http://ide:1614"
                                           "http://ide:8457"}
       (rsh/access-control-allow-methods) "OPTIONS, GET, POST, DELETE, PUT"
       (rsh/access-control-allow-credentials) true}
      1604
      {:keystore-file-path
        "/home/vladimir/workspace/certificate/jks/ide_server.jks"
       :keystore-password
        "ultras12"})
    (mon/mongodb-connect
      db-name)
    (ssn/create-indexes)
    (catch Exception e
      (println (.getMessage e))
     ))
 )

(defn stop-server
  "Stop server"
  []
  (try
    (srvr/stop-server)
    (mon/mongodb-disconnect)
    (catch Exception e
      (println (.getMessage e))
     ))
 )

(defn unset-restart-server
  "Stop server, unset server atom to nil
   reload project, start new server instance"
  []
  (stop-server)
  (use 'ide-server.core :reload)
  (start-server))

(defn -main [& args]
  (start-server))

