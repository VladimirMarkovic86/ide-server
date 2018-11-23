(ns ide-server.core
  (:gen-class)
  (:require [session-lib.core :as ssn]
            [server-lib.core :as srvr]
            [utils-lib.core :as utils :refer [parse-body]]
            [mongo-lib.core :as mon]
            [ide-server.scripts :as scripts]
            [ajax-lib.http.entity-header :as eh]
            [ajax-lib.http.response-header :as rsh]
            [ajax-lib.http.mime-type :as mt]
            [ajax-lib.http.status-code :as stc]
            [ide-middle.functionalities :as imfns]
            [ide-middle.request-urls :as irurls]
            [ide-middle.project.entity :as pem]
            [ide-middle.collection-names :refer [project-cname]]
            [common-server.core :as rt]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as cstring]
            [clojure.set :as cset]
            [audit-lib.core :refer [audit]]
            [utils-lib.core :as utils])
  (:import [java.io FileNotFoundException]))

(def db-uri
     (or (System/getenv "PROD_MONGODB")
         "mongodb://admin:passw0rd@127.0.0.1:27017/admin"))

(def db-name
     "ide-db")

(def stopped
     "stopped")

(def running
     "running")

(defn sh-exists?
  "Check if sh file exists"
  []
  (let [out (:out
              (sh
                "ls" "/tmp/sh"))]
    (not
      (empty?
        out))
   ))

(defn make-sh-file
  "Make sh file so it can be used in executing shell commands"
  []
  (when-not (sh-exists?)
    (try
      (sh "touch" "/tmp/sh")
      (sh "chmod" "755" "/tmp/sh")
      (let [file-path "/tmp/sh"
            file-content (str
                           "#!/bin/bash\n"
                           "for i do\n"
                           "  eval \"$i\"\n"
                           "done")
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
          os))
      (catch Exception e
        (println (.getMessage e))
       ))
   ))

(defn execute-shell-command
  "Execute shell command from sh file at it's file path"
  [command]
  (make-sh-file)
  (let [final-command (atom 
                        ["/tmp/sh"])
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

(defn build-uberjar
  "Build uberjar fetched by _id"
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
    (reset!
      output
      (execute-shell-command
        [(str
           "cd " absolute-path)
         "lein uberjar"]))
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str {:status "success"
                 :heading (str
                            "Build uberjar "
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

(defn git-diff-fn
  "Output git diff for multiple absolute paths"
  [request-body]
  (let [absolute-paths (:absolute-paths request-body)
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
                  diff-output (execute-shell-command
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
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :files-diffs @result})})
 )

(defn git-log-fn
  "Output git log for multiple absolute paths"
  [request-body]
  (let [absolute-paths (:absolute-paths request-body)
        result (atom [])]
    (doseq [absolute-path absolute-paths]
      (let [output (execute-shell-command
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
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :files-logs @result})})
 )

(defn git-unpushed-fn
  "Output git log for multiple absolute paths"
  [request-body]
  (let [absolute-paths (:absolute-paths request-body)
        result (atom [])]
    (doseq [absolute-path absolute-paths]
      (let [output (execute-shell-command
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
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :files-unpushed @result})})
 )

(defn git-commit-push-fn
  "Output git diff for multiple absolute paths"
  [request-body]
  (let [absolute-paths (:absolute-paths request-body)
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
                                    output (execute-shell-command
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
                  diff-output (execute-shell-command
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
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :changed-files @result})})
 )

(defn git-commit-push-action-fn
  "Execute commit push command"
  [websocket]
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
        (let [git-status-output (execute-shell-command
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
        (str
          {:action "update-progress"
           :progress-value 0}))
      (when (= pem/git-commit
               action)
        (doseq [root-path @changed-root-paths]
          (execute-shell-command
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
            (str
              {:action "update-progress"
               :progress-value (int
                                 (/ (* @progress-value
                                       100)
                                    (count
                                      @changed-root-paths))
                                )})
           ))
       )
      (when (= pem/git-commit-push
               action)
        (doseq [root-path @changed-root-paths]
          (execute-shell-command
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
            (str
              {:action "update-progress"
               :progress-value (int
                                 (/ (* @progress-value
                                       100)
                                    (count
                                      @changed-root-paths))
                                )})
           ))
       )
      (when (= pem/git-push
               action)
        (doseq [root-path @changed-root-paths]
          (execute-shell-command
            [(str
               "cd " root-path)
             "git push origin master"])
          (swap!
            progress-value
            inc)
          (websocket-output-fn
            (str
              {:action "update-progress"
               :progress-value (int
                                 (/ (* @progress-value
                                       100)
                                    (count
                                      @changed-root-paths))
                                )})
           ))
       )
      (websocket-output-fn
        (str
          {:action "update-progress"
           :progress-value 100}))
      (websocket-output-fn
        (str
          {:status "close"})
        -120))
    (catch Exception e
      (println (.getMessage e))
      ((:websocket-output-fn websocket)
        (str
          {:status "close"})
        -120))
   ))

(defn git-file-change-state-fn
  "Change file state in git add, remove or reset"
  [request-body]
  (try
    (let [{action :action
           absolute-path :absolute-path
           changed-file :changed-file} request-body]
      (when (= action
               pem/git-add)
        (execute-shell-command
          [(str
             "cd " absolute-path)
           (str
             "git add " changed-file)])
       )
      (when (= action
               pem/git-rm)
        (execute-shell-command
          [(str
             "cd " absolute-path)
           (str
             "git rm " changed-file)])
       )
      (when (= action
               pem/git-reset)
        (execute-shell-command
          [(str
             "cd " absolute-path)
           (str
             "git reset " changed-file)])
       )
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
               {:success "success"})})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
               {:success "error"
                :message (.getMessage e)})}
     ))
 )

(defn git-status-fn
  "HTTP response with git status command result on particular absolute path"
  [request-body]
  (let [absolute-path (:dir-path request-body)
        git-status-output (git-status
                            absolute-path)]
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"
              :data git-status-output})}
   ))

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
          project-name (:project
                         project-clj)
          project-version (:version
                            project-clj)
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
                dependent-project-name (:project
                                         dep-project-clj)
                dependent-project-version (:version
                                            dep-project-clj)
                dependencies-seq (:dependencies
                                   dep-project-clj)]
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
              dependencies (:dependencies
                             selected-project)
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
              selected-project (first selected-project)
              debug (swap!
                      result-set
                      disj
                      {:project dependent-project-name})
              dependencies (:dependencies
                             selected-project)
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
                  selected-project (first selected-project)
                  debug (swap!
                          result-set
                          disj
                          {:project dependent-project-name})
                  dependencies (atom
                                 (:dependencies
                                   selected-project))]
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

(defn versioning-project-response
  "Returns http response list of projects where version and dependencies
   should be changed and up to date, for multiple projects"
  [request-body]
  (try
    (let [result (versioning-project
                   request-body)]
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str {:status "success"
                   :result result})})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str {:status "error"
                   :error-message (.getMessage e)})}
     ))
 )

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
                           (:project
                             project-clj))
            project-version (:version
                              project-clj)
            dependencies (:dependencies
                           project-clj)
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
                              "project"
                              {:project-type "Library"})
        project-libraries (mon/mongodb-find
                            "project"
                            {:project-type "Library"})
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

(defn upgrade-versions-response
  "Returns http response with changed projects and their current versions"
  [request-body]
  (try
    (let [result (upgrade-versions
                   request-body)]
      {:status (stc/ok)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
               {:status "success"
                :result result})})
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
               {:status "error"
                :error-message (.getMessage e)})}
     ))
 )

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

(defn upgrade-versions-save-response
  "Changed versions in projects and their dependencies"
  [request-body]
  (try
    (upgrade-versions-save
      request-body)
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"})}
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
               {:status "error"
                :error-message (.getMessage e)})}
     ))
 )

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
        (execute-shell-command
          [(str
             "cd "
             absolute-path)
           "lein install"]))
     ))
 )

(defn upgrade-versions-build-response
  "Build projects with changed versions"
  [request-body]
  (try
    (upgrade-versions-build
      request-body)
    {:status (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body (str
             {:status "success"})}
    (catch Exception e
      (println (.getMessage e))
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
               {:status "error"
                :error-message (.getMessage e)})}
     ))
 )

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
    (let [ls-out (execute-shell-command
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
  "Find given text in selected files or in absolute-paths of particular file types
   .clj, .cljc, .cljs"
  [request-body]
  (try
    (let [absolute-paths (:absolute-paths request-body)
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
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
               {:status "success"
                :result @response-result})})
    (catch Exception e
      (println e)
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
               {:status "error"
                :error-message (.getMessage e)})}
     ))
 )

(defn projects-tree
  "Returns projects for project tree on front-end"
  [request-body]
  (try
    (let [entity-type (:entity-type request-body)
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
                (:out
                  output))
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
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str {:status "success"
                   :data @projects-a})})
    (catch Exception e
      (println (.getMessage e))
      (println e)
      {:status (stc/internal-server-error)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str
               {:status "Error"
                :message (.getMessage e)})}
     ))
 )

(defn response-routing-fn
  "Custom routing function"
  [request]
  (let [{request-uri :request-uri
         request-method :request-method} request]
    (cond
      (= request-method
         "ws GET")
        (cond
          (= request-uri
             irurls/git-commit-push-action-url)
            (git-commit-push-action-fn
              (:websocket request))
          :else
            nil)
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
             irurls/build-uberjar-url)
            (build-uberjar
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
            (git-status-fn
              (parse-body
                request))
          (= request-uri
             irurls/git-diff-url)
            (git-diff-fn
              (parse-body
                request))
          (= request-uri
             irurls/git-log-url)
            (git-log-fn
              (parse-body
                request))
          (= request-uri
             irurls/git-unpushed-url)
            (git-unpushed-fn
              (parse-body
                request))
          (= request-uri
             irurls/git-commit-push-url)
            (git-commit-push-fn
              (parse-body
                request))
          (= request-uri
             irurls/git-file-change-state-url)
            (git-file-change-state-fn
              (parse-body
                request))
          (= request-uri
             irurls/save-file-changes-url)
            (save-file-changes
              (parse-body
                request))
          (= request-uri
             irurls/versioning-project-url)
            (versioning-project-response
              (parse-body
                request))
          (= request-uri
             irurls/upgrade-versions-url)
            (upgrade-versions-response
              (parse-body
                request))
          (= request-uri
             irurls/upgrade-versions-save-url)
            (upgrade-versions-save-response
              (parse-body
                request))
          (= request-uri
             irurls/upgrade-versions-build-url)
            (upgrade-versions-build-response
              (parse-body
                request))
          (= request-uri
             irurls/find-text-in-files-url)
            (find-text-in-files-fn
              (parse-body
                request))
          (= request-uri
             irurls/projects-tree-url)
            (projects-tree
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
         "ws GET")
        (cond
          (= request-uri
             irurls/git-commit-push-action-url)
            (contains?
              allowed-functionalities
              imfns/git-commit-push-action)
          :else
            false)
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
             irurls/build-uberjar-url)
            (contains?
              allowed-functionalities
              imfns/build-uberjar)
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
             irurls/git-diff-url)
            (contains?
              allowed-functionalities
              imfns/git-diff)
          (= request-uri
             irurls/git-log-url)
            (contains?
              allowed-functionalities
              imfns/git-log)
          (= request-uri
             irurls/git-unpushed-url)
            (contains?
              allowed-functionalities
              imfns/git-unpushed)
          (= request-uri
             irurls/git-commit-push-url)
            (contains?
              allowed-functionalities
              imfns/git-commit-push)
          (= request-uri
             irurls/git-file-change-state-url)
            (contains?
              allowed-functionalities
              imfns/git-file-change-state)
          (= request-uri
             irurls/save-file-changes-url)
            (contains?
              allowed-functionalities
              imfns/save-file-changes)
          (= request-uri
             irurls/versioning-project-url)
            (contains?
              allowed-functionalities
              imfns/versioning-project)
          (= request-uri
             irurls/upgrade-versions-url)
            (contains?
              allowed-functionalities
              imfns/upgrade-versions)
          (= request-uri
             irurls/upgrade-versions-save-url)
            (contains?
              allowed-functionalities
              imfns/upgrade-versions-save)
          (= request-uri
             irurls/upgrade-versions-build-url)
            (contains?
              allowed-functionalities
              imfns/upgrade-versions-build)
          (= request-uri
             irurls/find-text-in-files-url)
            (contains?
              allowed-functionalities
              imfns/find-text-in-files)
          (= request-uri
             irurls/projects-tree-url)
            (contains?
              allowed-functionalities
              imfns/projects-tree)
          :else
            false)
      :else
        false))
 )

(defn routing
  "Routing function"
  [request]
  (let [response (rt/routing
                   request
                   response-routing-fn
                   allow-action-routing-fn)]
    (audit
      request
      response)
    response))

(defn start-server
  "Start server"
  []
  (try
    (let [port (System/getenv "PORT")
          port (if port
                 (read-string
                   port)
                 1604)
          access-control-allow-origin #{"https://ide:8455"
                                        "https://ide:1614"
                                        "http://ide:1614"
                                        "https://ide:1604"
                                        "http://ide:1604"
                                        "http://ide:8457"}
          access-control-allow-origin (if (System/getenv "CLIENT_ORIGIN")
                                        (conj
                                          access-control-allow-origin
                                          (System/getenv "CLIENT_ORIGIN"))
                                        access-control-allow-origin)
          access-control-map {(rsh/access-control-allow-origin) access-control-allow-origin
                              (rsh/access-control-allow-methods) "OPTIONS, GET, POST, DELETE, PUT"
                              (rsh/access-control-allow-credentials) true}
          certificates {:keystore-file-path
                         "certificate/ide_server.jks"
                        :keystore-password
                         "ultras12"}
          certificates (when-not (System/getenv "CERTIFICATES")
                         certificates)
          thread-pool-size (System/getenv "THREAD_POOL_SIZE")]
      (when thread-pool-size
        (reset!
          srvr/thread-pool-size
          (read-string
            thread-pool-size))
       )
      (srvr/start-server
        routing
        access-control-map
        port
        certificates))
    (mon/mongodb-connect
      db-uri
      db-name)
    (scripts/initialize-db-if-needed)
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

