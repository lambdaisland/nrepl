(ns lambdaisland.nrepl.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [lambdaisland.nrepl :as nrepl]))

(defn- accumulate-vector [m k v]
  (update m k (fnil conj []) v))

(defn- parse-kw
  [s]
  (if (.startsWith s ":") (read-string s) (keyword s)))

(def cli-options
  [["-p" "--port PORT" "Port to run nREPL on"
    :parse-fn #(Long/parseLong %)]
   ["-i" "--init-ns SYMBOL" "Initial namespace, defaults to `user`"
    :parse-fn symbol]
   ["-m" "--middleware SYMBOL" "Extra middleware to inject. Can be specified multiple times. Use a namespaced symbol."
    :parse-fn symbol
    :assoc-fn accumulate-vector]
   ["-b" "--bind HOST" "Host to bind the interface on. Default 127.0.0.1"]
   ["-g" "--greeting-fn SYMBOL" "Greeting function, called whenever a client connects."
    :parse-fn symbol]
   ["-s" "--silent" "Don't print the welcome message"]
   ["-H" "--nrepl-help" "Display this help message"]])

(defn help [args]
  (println "\nUSAGE:\n")
  (println "clj -m" (namespace `help) "[OPTIONS]\n")
  (println (:summary args))
  (System/exit (if (:errors args) 1 0)))

(defn sym->var [sym]
  (when sym
    (require (symbol (namespace sym)))
    (resolve sym)))

(defn -main [& args]
  (let [{:keys [errors options] :as args} (parse-opts args cli-options)]
    (when errors
      (run! println errors)
      (help args))

    (let [{:keys [nrepl-help silent bind port]} (merge nrepl/*default-opts* options)]
      (when nrepl-help
        (help args))

      (when-not silent
        (println "Starting nREPL server on" (str bind ":" port )))

      (doto (clojure.java.io/file ".nrepl-port")
        .deleteOnExit
        (spit port)))

    (nrepl/start-server (update options :greeting-fn sym->var))

    (Thread/sleep Long/MAX_VALUE)))
