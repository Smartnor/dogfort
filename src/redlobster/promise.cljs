(ns redlobster.promise
  (:require-macros [cljs.node-macros :as n])
  (:require [redlobster.events :as e]))

(defprotocol IPromise
  (realised? [this])
  (failed? [this])
  (realise [this value])
  (realise-error [this value])
  (on-realised [this on-success on-error]))

(deftype Promise [ee]
  IDeref
  (-deref [this]
    (let [realised (.-__realised ee)
          value (.-__value ee)]
      (cond
       (not realised) :redlobster.promise/not-realised
       :else value)))
  IPromise
  (realised? [this]
    (if (nil? (.-__realised ee)) false true))
  (failed? [this]
    (and (realised? this) (= "error" (.-__realised ee))))
  (realise [this value]
    (doto ee
      (aset "__realised" "success")
      (aset "__value" value)
      (e/emit :success [value])))
  (realise-error [this error]
    (doto ee
      (aset "__realised" "error")
      (aset "__value" error)
      (e/emit :error [error])))
  (on-realised [this on-success on-error]
    (if (realised? this)
      (if (failed? this) (on-error @this) (on-success @this))
      (doto ee
        (e/on :success on-success)
        (e/on :error on-error)))))

(defn promise
  ([]
     (Promise.
      (doto (e/event-emitter)
        (aset "__realised" nil)
        (aset "__value" nil))))
  ([on-success on-error]
     (on-realised (promise) on-success on-error)))

(defn promise? [v]
  (satisfies? IPromise v))