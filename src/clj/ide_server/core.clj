(ns ide-server.core
  (:gen-class)
  (:require [session-lib.core :as ssn]
            [server-lib.core :as srvr]
            [utils-lib.core :as utils]
            [utils-lib.core-clj :as cljutils]
            [mongo-lib.core :as mon]
            [ide-server.config :as config]
            [ide-server.scripts :as scripts]
            [ajax-lib.http.entity-header :as eh]
            [ajax-lib.http.response-header :as rsh]
            [ajax-lib.http.mime-type :as mt]
            [ajax-lib.http.status-code :as stc]
            [ajax-lib.http.request-method :as rm]
            [ide-middle.functionalities :as imfns]
            [ide-middle.request-urls :as irurls]
            [ide-middle.project.entity :as pem]
            [ide-middle.collection-names :refer [project-cname]]
            [common-server.core :as rt]
            [clojure.java.io :as io]
            [clojure.string :as cstring]
            [clojure.set :as cset]
            [audit-lib.core :refer [audit]])
  (:import [java.io FileNotFoundException]
           [java.util Base64]))

(def stopped
     "stopped")

(def running
     "running")

(defmethod rt/routing-fn
  [rm/GET
   irurls/video-url
   :logged-in
   :authorized]
  [request]
  "Stream video on GET request"
  (let [file-path (get-in
                    request
                    [:request-get-params
                     :filepath])
        last-dot-index (cstring/last-index-of
                         file-path
                         ".")
        extension (.substring
                    file-path
                    (inc
                      last-dot-index))]
    (srvr/read-file
      file-path
      extension
      request
      true))
 )

(defmethod rt/routing-fn
  [rm/ws-GET
   irurls/git-commit-push-action-url
   :logged-in
   :authorized]
  [request]
  "Execute commit push command"
  (let [websocket (:websocket request)]
    (try
      (let [{websocket-message :websocket-message
             websocket-output-fn :websocket-output-fn} websocket
            request-body (read-string
                           websocket-message)
            root-paths (:root-paths request-body)
            commit-message (:commit-message request-body)
            action (:action request-body)
            changed-root-paths (atom #{})
            progress-value (atom 0)]
        (doseq [root-path root-paths]
          (let [git-status-output (cljutils/execute-shell-command
                                    [(str
                                       "cd " root-path)
                                     "git status -s"])
                out (:out git-status-output)]
            (when-not (empty?
                        out)
              (let [changed-files (cstring/split
                                    out
                                    #"\n")
                    is-changed ((fn [index]
                                  (when (< index
                                           (count
                                             changed-files))
                                    (let [element (get
                                                    changed-files
                                                    index)
                                          md (.substring
                                               element
                                               1
                                               2)
                                          is-added-rm (= md
                                                         " ")]
                                      (if is-added-rm
                                        true
                                        (recur
                                          (inc
                                            index))
                                       ))
                                   ))
                                0)]
                (when is-changed
                  (swap!
                    changed-root-paths
                    conj
                    root-path))
               ))
           ))
        (websocket-output-fn
          {:action "update-progress"
           :progress-value 0})
        (when (= pem/git-commit
                 action)
          (doseq [root-path @changed-root-paths]
            (cljutils/execute-shell-command
              [(str
                 "cd " root-path)
               (str
                 "git commit -m \""
                 commit-message
                 "\"")])
            (swap!
              progress-value
              inc)
            (websocket-output-fn
              {:action "update-progress"
               :progress-value (int
                                 (/ (* @progress-value
                                       100)
                                    (count
                                      @changed-root-paths))
                                )})
           ))
        (when (= pem/git-commit-push
                 action)
          (doseq [root-path @changed-root-paths]
            (cljutils/execute-shell-command
              [(str
                 "cd " root-path)
               (str
                 "git commit -m '"
                 commit-message
                 "'")
               "git push origin master"])
            (swap!
              progress-value
              inc)
            (websocket-output-fn
              {:action "update-progress"
               :progress-value (int
                                 (/ (* @progress-value
                                       100)
                                    (count
                                      @changed-root-paths))
                                )})
           ))
        (when (= pem/git-push
                 action)
          (doseq [root-path @changed-root-paths]
            (cljutils/execute-shell-command
              [(str
                 "cd " root-path)
               "git push origin master"])
            (swap!
              progress-value
              inc)
            (websocket-output-fn
              {:action "update-progress"
               :progress-value (int
                                 (/ (* @progress-value
                                       100)
                                    (count
                                      @changed-root-paths))
                                )})
           ))
        (websocket-output-fn
          {:action "update-progress"
           :progress-value 100})
        (websocket-output-fn
          {:status "close"}
          -120))
      (catch Exception e
        (println (.getMessage e))
        ((:websocket-output-fn websocket)
          {:status "close"}
          -120))
     ))
 )

(defmethod rt/routing-fn
  [rm/POST
   irurls/read-file-url
   :logged-in
   :authorized]
  [request]
  "Read file if supported and return it in response as entity body"
  (try
    (let [request-body (:body request)
          file-path (:file-path request-body)
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
              #{"image"}
              operation)
        (let [f (java.io.File.
                  file-path)
              ary (byte-array
                    (.length
                      f))
              is (java.io.FileInputStream.
                   f)
              extension-start (cstring/last-index-of
                                file-path
                                ".")
              extension (.substring
                          file-path
                          (inc
                            extension-start)
                          (count
                            file-path))]
          (.read
            is
            ary)
          (.close
            is)
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
      (when (= operation
               "download")
        (let [f (java.io.File.
                  file-path)
              ary (byte-array
                    (.length
                      f))
              is (java.io.FileInputStream.
                   f)]
          (.read
            is
            ary)
          (.close
            is)
          (reset!
            headers
            {(eh/content-type) (mt/application-octet-stream)})
          (reset!
            body
            ary))
       )
      {:status (stc/ok)
       :headers @headers
       :body @body})
    (catch Exception e
      (println
        (.getMessage
          e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "error"
              :error-message (.getMessage e)}})
   ))

(defmethod rt/routing-fn
  [rm/POST
   irurls/execute-shell-command-url
   :logged-in
   :authorized]
  [request]
  "Execute shell command function with response"
  (let [request-body (:body request)
        command (:command request-body)
        output (cljutils/execute-shell-command
                 command)]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :data output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/list-documents-url
   :logged-in
   :authorized]
  [request]
  "List documents from dir-path"
  (let [request-body (:body request)
        dir-path (:dir-path request-body)
        output (cljutils/execute-shell-command
                 (str
                   "ls -al " dir-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :data output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/new-folder-url
   :logged-in
   :authorized]
  [request]
  "Make directory in dir-path"
  (let [request-body (:body request)
        dir-path (:dir-path request-body)
        output (cljutils/execute-shell-command
                 (str
                   "mkdir " dir-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :data output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/new-file-url
   :logged-in
   :authorized]
  [request]
  "Make file in file-path"
  (let [request-body (:body request)
        file-path (:file-path request-body)
        output (cljutils/execute-shell-command
                 (str
                   "touch " file-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :data output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/move-document-url
   :logged-in
   :authorized]
  [request]
  "Move document in dest-path"
  (let [request-body (:body request)
        doc-path (:doc-path request-body)
        dest-path (:dest-path request-body)
        output (cljutils/execute-shell-command
                 (str
                   "mv " doc-path " " dest-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :data output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/copy-document-url
   :logged-in
   :authorized]
  [request]
  "Copy document in dest-path"
  (let [request-body (:body request)
        doc-path (:doc-path request-body)
        dest-path (:dest-path request-body)
        output (cljutils/execute-shell-command
                 (str
                   "cp -r " doc-path " " dest-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :data output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/delete-document-url
   :logged-in
   :authorized]
  [request]
  "Delete document doc-path"
  (let [request-body (:body request)
        doc-path (:doc-path request-body)
        output (cljutils/execute-shell-command
                 (str
                   "rm -rf " doc-path))]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :data output}}))

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

(defmethod rt/routing-fn
  [rm/POST
   irurls/build-project-url
   :logged-in
   :authorized]
  [request]
  "Build project fetched by _id"
  (let [request-body (:body request)
        entity-id (:entity-id request-body)
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
        (cljutils/execute-shell-command
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
        (cljutils/execute-shell-command
          [(str
             "cd " absolute-path)
           "lein cljsbuild once dev"]))
     )
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :heading (str
                       "Build project "
                       (project-name
                         group-id
                         artifact-id
                         version))
            :data @output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/build-uberjar-url
   :logged-in
   :authorized]
  [request]
  "Build uberjar fetched by _id"
  (let [request-body (:body request)
        entity-id (:entity-id request-body)
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
    (reset!
      output
      (cljutils/execute-shell-command
        [(str
           "cd " absolute-path)
         "lein uberjar"]))
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :heading (str
                       "Build uberjar "
                       (project-name
                         group-id
                         artifact-id
                         version))
            :data @output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/build-project-dependencies-url
   :logged-in
   :authorized]
  [request]
  "Build project dependencies by _id"
  (let [request-body (:body request)
        entity-id (:entity-id request-body)
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
      (cljutils/execute-shell-command
        @build-command))
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :heading (str
                       "Build dependencies of "
                       m-group-id
                       "/"
                       m-artifact-id
                       "-"
                       m-version)
            :data @output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/clean-project-url
   :logged-in
   :authorized]
  [request]
  "Clean project by _id"
  (let [request-body (:body request)
        entity-id (:entity-id request-body)
        entity-type (:entity-type request-body)
        {group-id :group-id
         artifact-id :artifact-id
         version :version
         absolute-path :absolute-path
         language :language
         project-type :project-type} (mon/mongodb-find-by-id
                                       entity-type
                                       entity-id)
        output (cljutils/execute-shell-command
                 [(str
                    "cd " absolute-path)
                  "lein clean"
                  "rm -rf resources/public/assets/js/ resources/public/assets/jsprod/"])]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :heading (str
                       "Clean "
                       (project-name
                         group-id
                         artifact-id
                         version))
            :data output}}))

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
                out (cljutils/execute-shell-command
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
            (cljutils/execute-shell-command
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
                  out (cljutils/execute-shell-command
                        (str
                          "kill -9 " pid))]
              (reset!
                status
                (server-status-fn
                  request-body))
              (when (= @status
                       stopped)
                (cljutils/execute-shell-command
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

(defmethod rt/routing-fn
  [rm/POST
   irurls/run-project-url
   :logged-in
   :authorized]
  [request]
  "Interact with project wit start, stop, status and restart commands"
  (let [request-body (:body request)
        entity-id (:entity-id request-body)
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
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :heading (str
                       "Run "
                       (project-name
                         group-id
                         artifact-id
                         version))
            :data {:out @status}}
     }))

(defn git-status
  "Check git status of project"
  [root-dir]
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git status -s"])]
    output))

(defn git-init
  "Initialize git project if not initialized yet"
  [root-dir]
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git init"])]
    output))

(defn git-remote-add
  "Add remote git repository with ssh or https url"
  [root-dir
   git-remote-repo-link]
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git remote add origin " git-remote-repo-link)])]
    output))

(defn git-remote-remove
  "Remove remote repository url"
  [root-dir]
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git remote remove origin"])]
    output))

(defn git-add
  "Git add new file or file with changes"
  [root-dir
   file-path]
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git add " file-path)])]
    output))

(defn git-rm
  "Git remove file"
  [root-dir
   file-path]
  (let [output (cljutils/execute-shell-command
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
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git reset " file-path " -q")])]
    output))

(defn git-commit
  "Git commit changes"
  [root-dir
   commit-message]
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git commit -m '" commit-message "'")])]
    output))

(defn git-push
  "Git push commits to remote repository"
  [root-dir]
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git push"])]
    output))

(defn git-unpushed-commits
  "Check for unpushed commits"
  [root-dir]
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git log origin/master..HEAD --oneline"])]
    output))

(defn git-diff
  "Git diff of project"
  [root-dir]
  (let [output (cljutils/execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git diff"])]
    output))

(defmethod rt/routing-fn
  [rm/POST
   irurls/git-project-url
   :logged-in
   :authorized]
  [request]
  "Interact with project with git commands"
  (let [request-body (:body request)
        entity-id (:entity-id request-body)
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
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :git-remote-url (or new-git-remote-link
                                git-remote-link)
            :unpushed-commits @unpushed-commits
            :project-diff @project-diff
            :data @output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/git-status-url
   :logged-in
   :authorized]
  [request]
  "HTTP response with git status command result on particular absolute path"
  (let [request-body (:body request)
        absolute-path (:dir-path request-body)
        git-status-output (git-status
                            absolute-path)]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :data git-status-output}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/git-diff-url
   :logged-in
   :authorized]
  [request]
  "Output git diff for multiple absolute paths"
  (let [request-body (:body request)
        absolute-paths (:absolute-paths request-body)
        result (atom [])]
    (doseq [absolute-path absolute-paths]
      (let [changed-files (git-status
                            absolute-path)
            [absolute-path
             changed-files] (if (cstring/index-of
                                  (:err changed-files)
                                  "Not a directory")
                             (let [last-slash-index (cstring/last-index-of
                                                      absolute-path
                                                      "/")
                                   absolute-path-part1 (.substring
                                                         absolute-path
                                                         0
                                                         last-slash-index)
                                   absolute-path-part2 (.substring
                                                         absolute-path
                                                         (inc
                                                           last-slash-index)
                                                         (count
                                                           absolute-path))]
                               [absolute-path-part1
                                [(str
                                   "   "
                                   absolute-path-part2)]])
                            [absolute-path
                             (cstring/split
                               (:out changed-files)
                               #"\n")])]
        (doseq [changed-file changed-files]
          (when-not (cstring/index-of
                      changed-file
                      "../")
            (let [changed-file (.substring
                                 changed-file
                                 3
                                 (count
                                   changed-file))
                  diff-output (cljutils/execute-shell-command
                                [(str
                                   "cd " absolute-path)
                                 (str
                                   "git diff " changed-file)])
                  diff-output (:out diff-output)
                  file-name (cstring/split
                              changed-file
                              #"/")
                  file-name (last
                              file-name)
                  file-name (str
                              file-name
                              "_diff")]
              (when-not (empty?
                          diff-output)
                (swap!
                  result
                  conj
                  [(str
                     absolute-path
                     "/"
                     changed-file
                     "_diff")
                   file-name
                   diff-output]))
             ))
         ))
     )
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :files-diffs @result}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/git-log-url
   :logged-in
   :authorized]
  [request]
  "Output git log for multiple absolute paths"
  (let [request-body (:body request)
        absolute-paths (:absolute-paths request-body)
        result (atom [])]
    (doseq [absolute-path absolute-paths]
      (let [output (cljutils/execute-shell-command
                     [(str
                        "cd " absolute-path)
                      "git log"])
            out (:out output)
            file-name (last
                        (cstring/split
                          absolute-path
                          #"/"))
            file-name (str
                        file-name
                        ".commit_log")]
        (swap!
          result
          conj
          [(str
             absolute-path
             ".commit_log")
           file-name
           out]))
     )
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :files-logs @result}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/git-unpushed-url
   :logged-in
   :authorized]
  [request]
  "Output unpushed git commits for multiple absolute paths"
  (let [request-body (:body request)
        absolute-paths (:absolute-paths request-body)
        result (atom [])]
    (doseq [absolute-path absolute-paths]
      (let [output (cljutils/execute-shell-command
                     [(str
                        "cd " absolute-path)
                      "git log origin/master..HEAD --oneline"])
            out (:out output)
            file-name (last
                        (cstring/split
                          absolute-path
                          #"/"))
            file-name (str
                        file-name
                        ".commit_log")]
        (swap!
          result
          conj
          [(str
             absolute-path
             ".commit_log")
           file-name
           out]))
     )
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :files-unpushed @result}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/git-commit-push-url
   :logged-in
   :authorized]
  [request]
  "Output git changed files paths for multiple projects absolute paths"
  (let [request-body (:body request)
        absolute-paths (:absolute-paths request-body)
        result (atom [])]
    (doseq [absolute-path absolute-paths]
      (let [changed-files (git-status
                            absolute-path)
            [absolute-path
             changed-files] (if (cstring/index-of
                                  (:err changed-files)
                                  "Not a directory")
                              (let [last-slash-index (cstring/last-index-of
                                                       absolute-path
                                                       "/")
                                    absolute-path-part1 (.substring
                                                          absolute-path
                                                          0
                                                          last-slash-index)
                                    absolute-path-part2 (.substring
                                                          absolute-path
                                                          (inc
                                                            last-slash-index)
                                                          (count
                                                            absolute-path))
                                    output (cljutils/execute-shell-command
                                             [(str
                                                "cd " absolute-path-part1)
                                              (str
                                                "git status -s " absolute-path-part2)])
                                    status-out (:out output)]
                                (when-not (empty?
                                            status-out)
                                  [absolute-path-part1
                                   [status-out]]))
                              [absolute-path
                               (cstring/split
                                 (:out changed-files)
                                 #"\n")])]
        (doseq [changed-file changed-files]
          (when-not (cstring/index-of
                      changed-file
                      "../")
            (let [type-of-change (.substring
                                   changed-file
                                   0
                                   3)
                  mad (.substring
                        type-of-change
                        0
                        1)
                  md (.substring
                       type-of-change
                       1
                       2)
                  action (if-not (= mad
                                    " ")
                           mad
                           md)
                  checked (= md
                             " ")
                  changed-file (.substring
                                 changed-file
                                 3
                                 (count
                                   changed-file))
                  diff-output (cljutils/execute-shell-command
                                [(str
                                   "cd " absolute-path)
                                 (str
                                   "git status -s " changed-file)])
                  diff-output (:out diff-output)
                  ]
              (when-not (empty?
                          diff-output)
                (swap!
                  result
                  conj
                  [absolute-path
                   changed-file
                   checked
                   action]))
             ))
         ))
     )
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-clojurescript)}
     :body {:status "success"
            :changed-files @result}}))

(defmethod rt/routing-fn
  [rm/POST
   irurls/git-file-change-state-url
   :logged-in
   :authorized]
  [request]
  "Change file state in git add, remove or reset"
  (try
    (let [request-body (:body request)
          {action :action
           absolute-path :absolute-path
           changed-file :changed-file} request-body]
      (when (= action
               pem/git-add)
        (cljutils/execute-shell-command
          [(str
             "cd " absolute-path)
           (str
             "git add " changed-file)])
       )
      (when (= action
               pem/git-rm)
        (cljutils/execute-shell-command
          [(str
             "cd " absolute-path)
           (str
             "git rm " changed-file)])
       )
      (when (= action
               pem/git-reset)
        (cljutils/execute-shell-command
          [(str
             "cd " absolute-path)
           (str
             "git reset " changed-file)])
       )
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:success "success"}})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:success "error"
              :message (.getMessage e)}})
   ))

(defmethod rt/routing-fn
  [rm/POST
   irurls/save-file-changes-url
   :logged-in
   :authorized]
  [request]
  "Save file changes"
  (try
    (let [request-body (:body request)
          file-path (:file-path request-body)
          file-content (:file-content request-body)
          is-base64 (:is-base64 request-body)
          f (java.io.File.
              file-path)
          ary (if is-base64
                (let [splitted-base64 (cstring/split
                                        file-content
                                        #"base64,")
                      image-base64 (get
                                     splitted-base64
                                     1)]
                  (.decode
                    (Base64/Decoder/getDecoder)
                    image-base64))
                (.getBytes
                  file-content
                  "UTF-8"))
          os (java.io.FileOutputStream.
               f)]
      (.write
        os
        ary)
      (.close
        os)
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "success"}})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "error"
              :error-message (.getMessage e)}})
   ))

(defn project-clj-into-map
  "Makes clojure map out of project.clj file content"
  [file-path]
  (try
    (let [project-map (atom {})
          project-seq (rest
                        (read-string
                          (slurp
                            file-path))
                       )
          project-name (first
                         project-seq)
          project-seq (rest
                        project-seq)
          project-version (first
                            project-seq)
          project-seq (rest
                        project-seq)
          seq-key (atom nil)
          seq-val (atom nil)]
      (swap!
        project-map
        assoc
        :project
        (str
          project-name)
        :version
        project-version)
      (doseq [seq-el project-seq]
        (when (and @seq-key
                   (not @seq-val))
          (reset!
            seq-val
            seq-el))
        (when-not @seq-key
          (reset!
            seq-key
            seq-el))
        (when (and @seq-key
                   @seq-val)
          (swap!
            project-map
            assoc
            @seq-key
            @seq-val)
          (reset!
            seq-key
            nil)
          (reset!
            seq-val
            nil))
       )
      @project-map)
    (catch Exception e
      (println (.getMessage e))
      {}))
 )

(defn project-dependency-tree
  "Reads project.clj file on passed absolute-path parameter, and makes clojure map of it,
   and follows dependency tree to its core"
  [absolute-path]
  (try
    (let [project-clj (project-clj-into-map
                        (str
                          absolute-path
                          "/project.clj"))
          project-name (:project project-clj)
          project-version (:version project-clj)
          projects-vector (mon/mongodb-find
                            project-cname)
          result (atom #{})]
      (doseq [{absolute-path-dep :absolute-path} projects-vector]
        (when-not (= absolute-path-dep
                     absolute-path)
          (let [dep-project-clj (project-clj-into-map
                                  (str
                                    absolute-path-dep
                                    "/project.clj"))
                dependent-project-name (:project dep-project-clj)
                dependent-project-version (:version dep-project-clj)
                dependencies-seq (:dependencies dep-project-clj)]
            (doseq [[project-name-dep
                     project-version-dep] dependencies-seq]
              (let [project-name-dep (str
                                       project-name-dep)]
                (when (= project-name-dep
                         project-name)
                  (swap!
                    result
                    conj
                    {:project-name project-name
                     :project-version project-version
                     :dependency-used-version project-version-dep
                     :project-new-version ""
                     :dependent-project-name dependent-project-name
                     :dependent-project-version dependent-project-version
                     :dependent-project-absolute-path absolute-path-dep
                     :down-to-base (project-dependency-tree
                                     absolute-path-dep)}))
               ))
           ))
       )
      @result)
    (catch Exception e
      (println (.getMessage e))
      #{}))
 )

(defn down-to-base-fn
  "Updates result-set function parameter by accumulating dependencies,
   for particular project, which version should be updated"
  [down-to-base-param
   result-set]
  (when-not (empty?
              down-to-base-param)
    (doseq [{project-name :project-name
             project-version :project-version
             dependency-used-version :dependency-used-version
             project-new-version :project-new-version
             dependent-project-name :dependent-project-name
             dependent-project-version :dependent-project-version
             dependent-project-absolute-path :dependent-project-absolute-path
             down-to-base :down-to-base} down-to-base-param]
      (if (contains?
            @result-set
            {:project dependent-project-name})
        (let [selected-project (cset/select
                                 (fn [element]
                                   (= dependent-project-name
                                      (:project element))
                                  )
                                 @result-set)
              selected-project (first selected-project)
              debug (swap!
                      result-set
                      disj
                      {:project dependent-project-name})
              dependencies (:dependencies selected-project)
              dependencies (conj
                             dependencies
                             {:project project-name
                              :version project-version
                              :actual-version dependency-used-version})
              selected-project (assoc
                                 selected-project
                                 :dependencies
                                 dependencies)]
          (swap!
            result-set
            conj
            selected-project))
        (swap!
          result-set
          conj
          {:project dependent-project-name
           :version dependent-project-version
           :dependencies (sorted-set-by
                           (fn [{project1 :project}
                                {project2 :project}]
                             (compare
                               project1
                               project2))
                           {:project project-name
                            :version project-version
                            :actual-version dependency-used-version})})
       )
      (down-to-base-fn
        down-to-base
        result-set))
   ))

(defn versioning-project-concrete
  "Returns list of projects where version and dependencies
   should be changed and up to date, only for one project"
  [entity-id
   entity-type]
  (let [project-ent (mon/mongodb-find-by-id
                      entity-type
                      entity-id)
        absolute-path (:absolute-path project-ent)
        result (project-dependency-tree
                 absolute-path)
        result-set (atom
                     (sorted-set-by
                       (fn [{project-1 :project}
                            {project-2 :project}]
                         (compare
                           project-1
                           project-2))
                      ))]
    (doseq [{project-name :project-name
             project-version :project-version
             dependency-used-version :dependency-used-version
             project-new-version :project-new-version
             dependent-project-name :dependent-project-name
             dependent-project-version :dependent-project-version
             dependent-project-absolute-path :dependent-project-absolute-path
             down-to-base :down-to-base} result]
      (if (contains?
            @result-set
            {:project dependent-project-name})
        (let [selected-project (cset/select
                                 (fn [element]
                                   (= dependent-project-name
                                      (:project element))
                                  )
                                 @result-set)
              selected-project (first
                                 selected-project)
              debug (swap!
                      result-set
                      disj
                      {:project dependent-project-name})
              dependencies (:dependencies selected-project)
              dependencies (conj
                             dependencies
                             {:project project-name
                              :version project-version
                              :actual-version dependency-used-version})
              selected-project (assoc
                                 selected-project
                                 :dependencies
                                 dependencies)]
          (swap!
            result-set
            conj
            selected-project))
        (swap!
          result-set
          conj
          {:project dependent-project-name
           :version dependent-project-version
           :dependencies (sorted-set-by
                           (fn [{project1 :project}
                                {project2 :project}]
                             (compare
                               project1
                               project2))
                           {:project project-name
                            :version project-version
                            :actual-version dependency-used-version})})
       )
      (down-to-base-fn
        down-to-base
        result-set))
    @result-set))

(defn versioning-project
  "Returns list of projects where version and dependencies
   should be changed and up to date, for multiple projects"
  [request-body]
  (let [entity-ids (:entity-ids request-body)
        entity-type (:entity-type request-body)
        result (atom [])
        result-set (atom
                     (sorted-set-by
                       (fn [{project-1 :project}
                            {project-2 :project}]
                         (compare
                           project-1
                           project-2))
                      ))]
    (doseq [entity-id entity-ids]
      (swap!
        result
        conj
        (versioning-project-concrete
          entity-id
          entity-type))
     )
    (doseq [project-results @result]
      (doseq [project-result project-results]
        (let [{dependent-project-name :project
               dependent-project-version :version
               dependent-dependecies :dependencies} project-result]
          (if (contains?
                @result-set
                {:project dependent-project-name})
            (let [selected-project (cset/select
                                     (fn [element]
                                       (= dependent-project-name
                                          (:project element))
                                      )
                                     @result-set)
                  selected-project (first
                                     selected-project)
                  debug (swap!
                          result-set
                          disj
                          {:project dependent-project-name})
                  dependencies (atom
                                 (:dependencies selected-project))]
              (doseq [dependent-dependency dependent-dependecies]
                (swap!
                  dependencies
                  conj
                  dependent-dependency))                
              (swap!
                result-set
                conj
                (assoc
                  selected-project
                  :dependencies
                  @dependencies))
             )
            (swap!
              result-set
              conj
              project-result))
         ))
     )
    @result-set))

(defmethod rt/routing-fn
  [rm/POST
   irurls/versioning-project-url
   :logged-in
   :authorized]
  [request]
  "Returns http response list of projects where version and dependencies
   should be changed and up to date, for multiple projects"
  (try
    (let [request-body (:body request)
          result (versioning-project
                   request-body)]
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "success"
              :result result}})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "error"
              :error-message (.getMessage e)}})
   ))

(defn build-order-fn-recur
  "Library project build order recursion"
  [number-of-libraries
   project-libraries
   build-order
   built-projects]
  (when (< (count
             @build-order)
           number-of-libraries)
    (doseq [project-library project-libraries]
      (let [absolute-path (:absolute-path project-library)
            project-clj (project-clj-into-map
                          (str
                            absolute-path
                            "/project.clj"))
            project-name (str
                           (:project project-clj))
            project-version (:version project-clj)
            dependencies (:dependencies project-clj)
            ready-to-build (atom true)]
        (doseq [[dep-name
                 dep-version] dependencies]
          (when (and (clojure.string/index-of
                       dep-name
                       "org.clojars.vladimirmarkovic86/")
                     (not
                       (contains?
                         @built-projects
                         (str
                           dep-name))
                      ))
            (reset!
              ready-to-build
              false))
         )
        (when (and @ready-to-build
                   (not
                     (contains?
                       @built-projects
                       project-name))
               )
          (swap!
            build-order
            conj
            project-name)
          (swap!
            built-projects
            conj
            project-name))
       ))
    (recur
      number-of-libraries
      project-libraries
      build-order
      built-projects))
 )

(defn build-order-fn
  "Library project build order"
  []
  (let [number-of-libraries (mon/mongodb-count
                              project-cname
                              {:project-type pem/library})
        project-libraries (mon/mongodb-find
                            project-cname
                            {:project-type pem/library})
        build-order (atom [])
        built-projects (atom #{})]
    (build-order-fn-recur
      number-of-libraries
      project-libraries
      build-order
      built-projects)
    @build-order))

(defn upgrade-versions
  "Returns clojure vector with changed projects and their current versions"
  [request-body]
  (let [projects-with-dependencies (versioning-project
                                     request-body)
        build-order-vector (build-order-fn)
        dependencies-set (atom
                           (sorted-set-by
                             (fn [{project-1 :project}
                                  {project-2 :project}]
                               (compare
                                 project-1
                                 project-2))
                            ))
        result (atom [])]
    (doseq [{dependencies :dependencies} projects-with-dependencies]
      (doseq [dependency dependencies]
        (swap!
          dependencies-set
          conj
          dependency))
     )
    (doseq [project build-order-vector]
      (when (contains?
              @dependencies-set
              {:project project})
        (swap!
          result
          conj
          (get
            @dependencies-set
            {:project project}))
       ))
    @result))

(defmethod rt/routing-fn
  [rm/POST
   irurls/upgrade-versions-url
   :logged-in
   :authorized]
  [request]
  "Returns http response with changed projects and their current versions"
  (try
    (let [request-body (:body request)
          result (upgrade-versions
                   request-body)]
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "success"
              :result result}})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "error"
              :error-message (.getMessage e)}})
   ))

(defn upgrade-versions-save
  "Changed versions in projects and their dependencies"
  [request-body]
  (let [request-projects (:projects request-body)
        all-projects (mon/mongodb-find
                       "project")]
    (doseq [{absolute-path :absolute-path} all-projects]
      (let [file-path (str
                        absolute-path
                        "/project.clj")]
        (doseq [{group-id :group-id
                 artifact-id :artifact-id
                 new-version :new-version} request-projects]
          (let [file-content (slurp
                               (clojure.java.io/file
                                 file-path))
                project-name (str
                               group-id
                               "/"
                               artifact-id)
                project-index (cstring/index-of
                                file-content
                                project-name)]
            (when project-index
              (let [project-index (+ project-index
                                     (count
                                       project-name))
                    part-one (.substring
                               file-content
                               0
                               project-index)
                    part-two (.substring
                               file-content
                               project-index
                               (count
                                 file-content))
                    [part-two1
                     part-two2
                     part-two3] (cstring/split
                                  part-two
                                  #"\""
                                  3)
                    changed-file-content (str
                                           part-one
                                           part-two1
                                           "\"" new-version "\""
                                           part-two3)]
                (with-open [wr (io/writer
                                 file-path)]
                  (try
                    (.write
                      wr
                      changed-file-content)
                    (catch Exception e
                      (println e))
                   ))
               ))
           ))
       ))
   ))

(defmethod rt/routing-fn
  [rm/POST
   irurls/upgrade-versions-save-url
   :logged-in
   :authorized]
  [request]
  "Changed versions in projects and their dependencies"
  (try
    (let [request-body (:body request)]
      (upgrade-versions-save
        request-body)
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "success"}})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "error"
              :error-message (.getMessage e)}})
   ))

(defn upgrade-versions-build
  "Build projects with changed versions"
  [request-body]
  (let [request-projects (:projects request-body)]
    (doseq [{group-id :group-id
             artifact-id :artifact-id
             new-version :new-version} request-projects]
      (let [{absolute-path :absolute-path} (mon/mongodb-find-one
                                             "project"
                                             {:group-id group-id
                                              :artifact-id artifact-id})]
        (cljutils/execute-shell-command
          [(str
             "cd "
             absolute-path)
           "lein install"]))
     ))
 )

(defmethod rt/routing-fn
  [rm/POST
   irurls/upgrade-versions-build-url
   :logged-in
   :authorized]
  [request]
  "Build projects with changed versions"
  (try
    (let [request-body (:body request)]
      (upgrade-versions-build
        request-body)
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "success"}})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "error"
              :error-message (.getMessage e)}})
   ))

(defn is-file-extension-supported?
  "Returns true if file extension is supported"
  [doc-name]
  (let [ext-types ["clj"
                   "cljc"
                   "cljs"
                   "html"
                   "css"]
        is-supproted ((fn [index]
                        (when (< index
                                 (count
                                   ext-types))
                          (let [ext-type (get
                                           ext-types
                                           index)
                                ext-start-index (if (<= (count
                                                          doc-name)
                                                        (count
                                                          ext-type))
                                                  0
                                                  (- (count
                                                       doc-name)
                                                     (count
                                                       ext-type))
                                                 )
                                ext-end-index (count
                                                doc-name)
                                extension (.substring
                                            doc-name
                                            ext-start-index
                                            ext-end-index)]
                            (if (= ext-type
                                   extension)
                              true
                              (recur
                                (inc
                                  index))
                             ))
                         ))
                       0)]
    is-supproted))

(defn iterate-into-depth
  "Iterate into depth to find supproted files"
  [absolute-paths
   sub-files]
  (doseq [absolute-path absolute-paths]
    (let [ls-out (cljutils/execute-shell-command
                   [(str
                      "ls -al " absolute-path)])
          ls-out (:out ls-out)
          ls-out (clojure.string/split
                   ls-out
                   #"\n")
          sub-dirs (atom [])
          sub-files (or sub-files
                        (atom []))
          parse-doc-name (fn [line]
                           (let [separators-count (atom 0)
                                 previous-char (atom nil)
                                 doc-name (atom "")]
                             (doseq [c-char line]
                               (when (> @separators-count
                                        7)
                                 (swap!
                                   doc-name
                                   str
                                   c-char))
                               (when (and (= c-char
                                             \space)
                                          (not= @previous-char
                                                \space))
                                 (swap!
                                   separators-count
                                   inc))
                               (reset!
                                 previous-char
                                 c-char))
                             @doc-name))]
      (doseq [document-ls-line ls-out]
        (let [doc-name (parse-doc-name
                         document-ls-line)]
          (when-not (contains?
                      #{"."
                        ".."
                        ""
                        "target"
                        ".git"}
                      doc-name)
            (if (= (first
                     document-ls-line)
                   \d)
              (swap!
                sub-dirs
                conj
                (str
                  absolute-path
                  "/"
                  doc-name))
              (when (is-file-extension-supported?
                      doc-name)
                (if (= (first
                         doc-name)
                       \/)
                  (swap!
                    sub-files
                    conj
                    doc-name)
                  (swap!
                    sub-files
                    conj
                    (str
                      absolute-path
                      "/"
                      doc-name))
                 ))
             ))
         ))
      (iterate-into-depth
        @sub-dirs
        sub-files))
   )
  @sub-files)

(defn find-text-in-files-fn
  
  [request]
  )

(defmethod rt/routing-fn
  [rm/POST
   irurls/find-text-in-files-url
   :logged-in
   :authorized]
  [request]
  "Find given text in selected files or in absolute-paths of particular file types
   .clj, .cljc, .cljs"
  (try
    (let [request-body (:body request)
          absolute-paths (:absolute-paths request-body)
          find-this-text (:find-this-text request-body)
          sub-files (iterate-into-depth
                      absolute-paths
                      (atom []))
          response-result (atom "")]
      (doseq [sub-file sub-files]
        (let [file-content (slurp
                             (clojure.java.io/file
                               sub-file))
              file-content-a (atom
                               file-content)
              index-of-a (atom
                           -1)]
          (while @index-of-a
            (when (not= -1
                        @index-of-a)
              (let [start-index (if (< @index-of-a
                                       30)
                                  0
                                  (- @index-of-a
                                     30))
                    end-index (if (< (+ @index-of-a
                                        30)
                                     (count
                                       @file-content-a))
                                (+ @index-of-a
                                   30)
                                (count
                                  @file-content-a))
                    splited-text (utils/split-with-newline
                                   @file-content-a)
                    count-chars (atom 0)
                    line-num ((fn [index]
                                (when (< index
                                         (count
                                           splited-text))
                                  (let [row-text (get
                                                   splited-text
                                                   index)
                                        row-length (count
                                                     row-text)]
                                    (swap!
                                      count-chars
                                      +
                                      row-length)
                                    (if (< @index-of-a
                                           @count-chars)
                                      (inc
                                        index)
                                      (recur
                                        (inc
                                          index))
                                     ))
                                 ))
                               0)]
                (swap!
                  response-result
                  str
                  "file: " sub-file "\n"
                  "line: " line-num "\n"
                  (.substring
                    @file-content-a
                    start-index
                    end-index) "\n"))
             )
            (reset!
              index-of-a
              (clojure.string/index-of
                @file-content-a
                find-this-text
                (inc
                  @index-of-a))
             ))
         ))
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "success"
              :result @response-result}})
    (catch Exception e
      (println e)
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "error"
              :error-message (.getMessage e)}})
   ))

(defmethod rt/routing-fn
  [rm/POST
   irurls/projects-tree-url
   :logged-in
   :authorized]
  [request]
  "Returns projects for project tree on front-end"
  (try
    (let [request-body (:body request)
          entity-type (:entity-type request-body)
          entity-filter (:entity-filter request-body)
          qsort (:qsort request-body)
          db-result (mon/mongodb-find
                      entity-type
                      entity-filter
                      nil
                      qsort)
          projects-a (atom
                       [])]
      (doseq [project-el db-result]
        (let [{absolute-path :absolute-path} project-el
              output (git-status
                       absolute-path)]
          (if (empty?
                (:out output))
            (swap!
              projects-a
              conj
              (assoc
                project-el
                :changed
                false))
            (swap!
              projects-a
              conj
              (assoc
                project-el
                :changed
                true))
           ))
       )
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "success"
              :data @projects-a}})
    (catch Exception e
      (println (.getMessage e))
      (println e)
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-clojurescript)}
       :body {:status "Error"
              :message (.getMessage e)}})
   ))

(defn routing
  "Routing function"
  [request]
  (let [response (rt/routing
                   request)]
    (when @config/audit-action-a
      (audit
        request
        response))
    response))

(defn start-server
  "Start server"
  []
  (try
    (let [port (config/define-port)
          access-control-map (config/build-access-control-map)
          certificates-map (config/build-certificates-map)]
      (config/set-thread-pool-size)
      (config/set-audit)
      (srvr/start-server
        routing
        access-control-map
        port
        certificates-map))
    (mon/mongodb-connect
      config/db-uri
      config/db-name)
    (scripts/initialize-db-if-needed)
    (ssn/create-indexes)
    (config/add-custom-entities-to-entities-map)
    (config/set-report-paths)
    (config/read-sign-up-roles)
    (config/setup-e-mail-account)
    (config/setup-e-mail-templates-path)
    (config/bind-set-specific-preferences-fn)
    (config/bind-specific-functionalities-by-url)
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

