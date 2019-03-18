(ns ide-server.config
  (:require [ajax-lib.http.response-header :as rsh]
            [server-lib.core :as srvr]))

(def db-uri
     (or (System/getenv "MONGODB_URI")
         (System/getenv "PROD_MONGODB")
         "mongodb://admin:passw0rd@127.0.0.1:27017/admin"))

(def db-name
     "ide-db")

(defn define-port
  "Defines server's port"
  []
  (let [port (System/getenv "PORT")
        port (if port
               (read-string
                 port)
               1604)]
    port))

(defn build-access-control-map
  "Build access control map"
  []
  (let [access-control-allow-origin #{"https://ide:8455"
                                      "https://ide:1614"
                                      "http://ide:1614"
                                      "https://192.168.1.86:1614"
                                      "http://192.168.1.86:1614"
                                      "https://ide:1604"
                                      "http://ide:1604"
                                      "https://192.168.1.86:1604"
                                      "http://192.168.1.86:1604"
                                      "http://ide:8457"}
        access-control-allow-origin (if (System/getenv "CLIENT_ORIGIN")
                                      (conj
                                        access-control-allow-origin
                                        (System/getenv "CLIENT_ORIGIN"))
                                      access-control-allow-origin)
        access-control-allow-origin (if (System/getenv "SERVER_ORIGIN")
                                      (conj
                                        access-control-allow-origin
                                        (System/getenv "SERVER_ORIGIN"))
                                      access-control-allow-origin)
        access-control-map {(rsh/access-control-allow-origin) access-control-allow-origin
                            (rsh/access-control-allow-methods) "OPTIONS, GET, POST, DELETE, PUT"
                            (rsh/access-control-allow-credentials) true
                            (rsh/access-control-allow-headers) "Content-Type"}]
    access-control-map))

(defn build-certificates-map
  "Build certificates map"
  []
  (let [certificates {:keystore-file-path
                       "certificate/ide_server.jks"
                      :keystore-password
                       "ultras12"}
        certificates (when-not (System/getenv "CERTIFICATES")
                       certificates)]
    certificates))

(defn set-thread-pool-size
  "Set thread pool size"
  []
  (let [thread-pool-size (System/getenv "THREAD_POOL_SIZE")]
    (when thread-pool-size
      (reset!
        srvr/thread-pool-size
        (read-string
          thread-pool-size))
     ))
 )

