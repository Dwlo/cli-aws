(ns clj-aws.ec2.ec2-client
  (:require [clojure
             [string                :as string]
             [walk                  :as walk]]
            [clj-aws.provider.shell :as sh]
            [cheshire.core          :as cc]))


(defn- not-blank? [s] ((complement clojure.string/blank?) s))
(def intance-types {:t2-micro "t2.micro"})
(def is-spot-instance? (fn [instance] (= "spot" (:InstanceLifecycle instance))))
(def is-instance-of-type? (fn [type] (fn [instance] (= type (:InstanceType instance)))))
(def is-instance-running? (fn [instance] (= "running" (get-in instance [:State :Name]))))
(def has-no-tags? (fn [instance] (empty? (:Tags instance))))
(def is-instance-in-state? (fn [state] (fn [instance] (= state (get-in instance [:State :Name])))))
(def has-tag? (fn [tag] (fn [instance] (some
                                      #(and (= tag (:Key %))
                                            (not-blank? (:Value %)))
                                      (:Tags instance)))))
(def has-tags? (fn [tags] (fn [instance] (reduce #(and %1 ((has-tag? %2) instance)) true tags))))

;; (defn has-tag-with-value? [instance tag value] (->> (filter #(and (= tag   (:Key %))
;;                                                                  (= value (:Value %)))
;;                                                            (:Tags instance))
;;                                                    (seq)))



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

(defn add-tags
  "Adds or overwrites one or more tags for the specified Amazon EC2 resource or resources.
  Each resource can have a maximum of 50 tags. Each tag consists of a key and optional value.
  Tag keys must be unique per resource.
  https://docs.aws.amazon.com/cli/latest/reference/ec2/create-tags.html"
  [tags resources]
  (let [rces    (clojure.string/join " " resources)
        payload (cc/generate-string tags)]
    (sh/exec (str "aws ec2 create-tags --resources " rces  " --tags '" payload "'"))))

(defn delete-tags
  "Deletes the specified set of tags from the specified set of resources.
  https://docs.aws.amazon.com/cli/latest/reference/ec2/delete-tags.html"
  [tags resources]
  (let [rces (clojure.string/join " " resources)
        tgs  (str " Key=" (clojure.string/join " Key=" tags))]
    (sh/exec (str "aws ec2 delete-tags --resources " rces  " --tags " tgs))))

(defn list-all-tags
  "Lists all tags used for instances"
  []
  (->> (list-instances)
       (map :Tags)
       (filter (complement nil?))
       (map #(map :Key %))
       (reduce concat)
       distinct
       sort))

(defn get-web-link
  "Returns a http link to amazon web console with the list of given instances"
  [instances-ids]
  (->> (clojure.string/join "," instances-ids)
       (str "https://console.aws.amazon.com/ec2/v2/home?region=us-east-1#Instances:search=")))
