(ns clj-aws.ec2.ec2-client
  (:require [clojure
             [string                :as string]
             [walk                  :as walk]]
            [clj-aws.provider.shell :as sh]))


(defn- not-blank? [s] ((complement clojure.string/blank?) s))
(def intance-types {:t2-micro "t2.micro"})
(def is-spot-instance? (fn [instance] (= "spot" (:InstanceLifecycle instance))))
(def is-instance-of-type? (fn [type] (fn [instance] (= type (:InstanceType instance)))))
(def is-instance-running? (fn [instance] (= "running" (get-in instance [:State :Name]))))
(def has-no-tags? (fn [instance] (empty? (:Tags instance))))
(def has-tag? (fn [tag] (fn [instance] (some
                                      #(and (= tag (:Key %))
                                            (not-blank? (:Value %)))
                                      (:Tags instance)))))
(def has-tags? (fn [tags] (fn [instance] (reduce #(and %1 ((has-tag? %2) instance)) true tags))))



(defn describe-instances
  "Describes all instances."
  ([]
   (walk/keywordize-keys (sh/exec (str "aws ec2 describe-instances --no-paginate"))))
  ([instance-ids]
   (walk/keywordize-keys (sh/exec (str "aws ec2 describe-instances --instance-ids " (string/join " " instance-ids))))))

(defn list-instances
  "Lists all instances"
  ([]
   (->> (get-in (describe-instances) [:Reservations])
        (map #(get-in % [:Instances]))
        (apply concat)
        (into [])))
  ([instance-ids]
   (->> (get-in (describe-instances instance-ids) [:Reservations])
        (map #(get-in % [:Instances]))
        (apply concat)
        (into []))))

(defn search-instances
  "Queries instances with a search options"
  [predicates]
  (->> (list-instances)
       (filter (apply every-pred predicates))))
