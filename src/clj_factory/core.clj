(ns clj-factory.core)

;; # Counter functions

(def ^:dynamic *counters* (ref {}))

(defn set-counter!
  "Set the counter for the supplied key to the new value"
  [type value]
  (get
   (dosync
    (alter
     *counters*
     (fn [m] (assoc m type value))))
   type))

(defn reset-counter!
  "Reset the counter for the supplied key to 0"
  [type]
  (set-counter! type 0))

(defn get-counter
  "Get the counter for the supplied key"
  [type]
  (get @*counters* type 0))

(defn inc-counter!
  "Increment the counter for the supplied key by 1"
  [type]
  (dosync
   (alter
    *counters*
    (fn [m]
      (assoc m type (inc (get m type 0)))))))

(defn next-counter!
  "Increment and return the next value for the supplied key"
  [type]
  (get (inc-counter! type) type))

;; # Multimethods

(defmulti fseq (fn [type & _] type))

(defmulti factory (fn [type & _] type))

(defn eval-keys
  [[k v]]
  [k (if (ifn? v)
       (v) v)])

(defmacro deffactory
  [type opts & body]
  `(defmethod clj-factory.core/factory ~type
     [type# & args#]
     (apply merge
            (if (= (class ~type) Class) (eval (list 'new ~type)))
            (into {} (map eval-keys ~opts))
            args#)))

(defmacro defseq
  [type let-form result]
  `(let [type# ~type]
     (defmethod clj-factory.core/fseq type#
       [type#]
       (let [~let-form [(next-counter! type#)]]
         ~result))))
