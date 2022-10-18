(ns lambdaisland.greet
  (:require [clojure.tools.nrepl.transport :as transport]
            [clojure.tools.nrepl.middleware :as mw]))

(defn wrap-greeting [handler]
  (fn [{:keys [id op transport] :as request}]
    (when (= op "out-subscribe")
      (transport/send transport {:id id
                                 :out (str "hello, earthling " id)}))
    (handler request)))

(mw/set-descriptor! #'wrap-greeting
                    {:requires #{}
                     :expects #{}
                     :handles {}})
