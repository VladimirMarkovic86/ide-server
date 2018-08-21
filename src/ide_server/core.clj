(ns ide-server.core
  (:require [session-lib.core :as ssn]
            [server-lib.core :as srvr]
            [utils-lib.core :as utils]
            [mongo-lib.core :as mon]
            [dao-lib.core :as dao]
            [ajax-lib.http.entity-header :as eh]
            [ajax-lib.http.response-header :as rsh]
            [ajax-lib.http.mime-type :as mt]
            [ajax-lib.http.status-code :as stc]
            [ide-middle.project.entity :as pem]
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
  ""
  [command]
  (let [final-command (atom ["./sh"])
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
  ""
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
  ""
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
  ""
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
  ""
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
  ""
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
  ""
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
  ""
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
  ""
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
  ""
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
  ""
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
  ""
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
  ""
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
  ""
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
        (when (and (= language
                      pem/clojure)
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
  ""
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
          (when (and (= language
                        pem/clojure)
                     (= project-type
                        pem/application))
            (execute-shell-command
              [(str
                 "cd " absolute-path)
               "lein trampoline run &> /dev/null & echo $! >pid.file &"])
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
  ""
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
          (when (and (= language
                        pem/clojure)
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
  ""
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
  ""
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
              :data [@status]})})
 )

(defn git-project
  ""
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
  ""
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git status -s"])]
    output))

(defn git-init
  ""
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git init"])]
    output))

(defn git-remote-add
  ""
  [root-dir
   git-remote-repo-link]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git remote add origin " git-remote-repo-link)])]
    output))

(defn git-remote-remove
  ""
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git remote remove origin"])]
    output))

(defn git-add
  ""
  [root-dir
   file-path]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git add " file-path)])]
    output))

(defn git-rm
  ""
  [root-dir
   file-path]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git rm " file-path)])]
    output))

(defn git-reset
  ""
  [root-dir
   file-path]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git reset " file-path " -q")])]
    output))

(defn git-commit
  ""
  [root-dir
   commit-message]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  (str
                    "git commit -m '" commit-message "'")])]
    output))

(defn git-push
  ""
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git push"])]
    output))

(defn git-unpushed-commits
  ""
  [root-dir]
  (let [output (execute-shell-command
                 [(str
                    "cd " root-dir)
                  "git log origin/master..HEAD --oneline"])]
    output))

(defn git-project
  ""
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
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :git-remote-url (or new-git-remote-link
                                  git-remote-link)
              :unpushed-commits @unpushed-commits
              :data @output})})
 )

(defn not-found
  "Requested action not found"
  []
  {:status (stc/not-found)
   :headers {(eh/content-type) (mt/text-plain)}
   :body (str {:status "error"
               :error-message "404 not found"})})

(defn parse-body
  "Read entity-body from request, convert from string to clojure data"
  [request]
  (read-string
    (:body request))
 )

(defn routing
  "Routing function"
  [request-start-line
   request]
  (println
    (str
      "\n"
      (dissoc
        request
        :body))
   )
  (if (ssn/am-i-logged-in-fn request)
    (let [[cookie-key
           cookie-value] (ssn/refresh-session
                           request)
          response
           (case request-start-line
             "POST /am-i-logged-in" (ssn/am-i-logged-in
                                      request)
             "POST /get-entities" (dao/get-entities
                                    (parse-body
                                      request))
             "POST /get-entity" (dao/get-entity
                                  (parse-body
                                    request))
             "POST /update-entity" (dao/update-entity
                                     (parse-body
                                       request))
             "POST /insert-entity" (dao/insert-entity
                                     (parse-body
                                       request))
             "DELETE /delete-entity" (dao/delete-entity
                                       (parse-body
                                         request))
             "POST /logout" (ssn/logout
                              request)
             "POST /read-file" (read-file
                                 (parse-body
                                   request))
             "POST /execute-shell-command" (execute-shell-command-fn
                                             (parse-body
                                               request))
             "POST /list-documents" (list-documents-fn
                                      (parse-body
                                        request))
             "POST /new-folder" (mkdir-fn
                                  (parse-body
                                    request))
             "POST /new-file" (mkfile-fn
                                (parse-body
                                  request))
             "POST /move-document" (move-document-fn
                                     (parse-body
                                       request))
             "POST /copy-document" (copy-document-fn
                                     (parse-body
                                       request))
             "POST /delete-document" (delete-document-fn
                                       (parse-body
                                         request))
             "POST /build-project" (build-project
                                     (parse-body
                                       request))
             "POST /build-project-dependencies" (build-project-dependencies
                                                  (parse-body
                                                    request))
             "POST /clean-project" (clean-project
                                     (parse-body
                                       request))
             "POST /run-project" (run-project
                                   (parse-body
                                     request))
             "POST /git-project" (git-project
                                   (parse-body
                                     request))
             "POST /git-status" (git-status
                                  (parse-body
                                    request))
             {:status (stc/not-found)
              :headers {(eh/content-type) (mt/text-plain)}
              :body (str {:status  "success"})})]
      (update-in
        response
        [:headers]
        assoc
        cookie-key
        cookie-value))
    (case request-start-line
      "POST /login" (ssn/login-authentication
                      (parse-body
                        request)
                      (:user-agent request))
      "POST /sign-up" (dao/insert-entity
                        (parse-body
                          request))
      "POST /am-i-logged-in" (ssn/am-i-logged-in
                               request)
      {:status (stc/unauthorized)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
              {:status  "success"
               :logged-in false})})
   ))

(defn start-server
  "Start server"
  []
  (try
    (srvr/start-server
      routing
      {(rsh/access-control-allow-origin) #{"https://ide:8455"
                                           "http://ide:8457"
                                           "https://192.168.1.86:8455"}
       (rsh/access-control-allow-methods) "GET, POST, DELETE, PUT"}
      1608)
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

