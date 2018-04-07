(ns lambdaisland.nrepl
  (:require [clojure.tools.nrepl.server :as nrepl]
            [clojure.tools.nrepl.middleware :as nrepl-mw]
            [clojure.tools.nrepl.middleware.session :as nrepl-mw-session]))

(def ^:dynamic *default-opts*
  {:bind "127.0.0.1"
   :port 7888
   :init-ns 'user})

(defn- wrap-init-vars
  "Make sure certain vars are set inside the nREPL session.

  In particular this

  - tries to load user.clj (or a different init-ns)
  - sets it as the initial *ns*
  - loads add data_readers.edn on the classpath
  - propagates the top level *data-readers* into the nREPL session

  This code has made its way in various shapes from Leiningen to Boot, and
  finally here. It adds an extra middleware to nREPL. It uses `with-local-vars`
  because Leiningen expects middleware to be vars.

  It uses `with-local-vars` a second time, because nREPL's session works by
  persisting vars across messages."

  ;; https://github.com/technomancy/leiningen/blob/master/src/leiningen/repl.clj
  ;; https://github.com/boot-clj/boot/blob/master/boot/pod/src/boot/repl_server.clj

  [init-ns]
  (with-local-vars
    [wrap-init-vars'
     (fn [handler]
       ;; this needs to be a var, since it's in the nREPL session
       (with-local-vars [init-ns-sentinel nil]
         (fn [{:keys [session] :as msg}]
           (when-not (@session init-ns-sentinel)
             (#'clojure.core/load-data-readers)
             (swap! session assoc
                    init-ns-sentinel      true
                    (var *data-readers*)  (.getRawRoot #'*data-readers*)
                    (var *ns*)            (try (require init-ns)
                                               (create-ns init-ns)
                                               (catch Throwable t
                                                 (.printStackTrace t)
                                                 (create-ns 'user)))))
           (handler msg))))]
    (doto wrap-init-vars'
      ;; set-descriptor! currently nREPL only accepts a var
      (nrepl-mw/set-descriptor!
       {:requires #{#'nrepl-mw-session/session}
        :expects #{"eval"}})
      (alter-var-root (constantly @wrap-init-vars')))))


(defn require+resolve
  "Given a namespaced symbol, load the namespace, then resolve the var."
  [sym]
  {:pre [(qualified-symbol? sym)]}
  (require (symbol (namespace sym)))
  (resolve sym))

(defn expand-mw
  "Recursively load/resolve and expand the list of middleware.

  Middleware is specified on the command line with symbols which need to be
  loaded and resolved, these symbols might actually point to sequences of
  middleware, rather than being middleware themselves. Expand this so we can
  pass it to nrepl/default-handler."
  [vars]
  (sequence
   (comp (map #(cond-> % (symbol? %) require+resolve))
         (map #(if (sequential? (deref %))
                 (expand-mw (deref %))
                 [%]))
         cat)
   vars))

(defn start-server
  "Convenience wrapper around clojure.tools.nrepl/start-server.

  Options

  - `:port`       Port to listen on
  - `:bind`       Interface (host) to bind on
  - `:middleware` Extra middleware to load, seq of symbols/vars
  - `:init-ns`    Namespace (symbol) to load and use as initial namespace"
  [options]
  (let [{:keys [port bind middleware init-ns]} (merge *default-opts* options)]
    (let [middleware (-> (wrap-init-vars init-ns)
                         (cons middleware)
                         expand-mw)
          handler    (apply nrepl/default-handler middleware)]
      (nrepl/start-server :bind bind
                          :port port
                          :handler handler))))
