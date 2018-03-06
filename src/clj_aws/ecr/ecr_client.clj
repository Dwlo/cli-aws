(ns clj-aws.ecr.ecr-client
  (:require [clojure
             [string                :as string]
             [walk                  :as walk]]
            [clj-aws.provider.shell :as sh]
            [cheshire.core          :as cc]))


(defn list-images
  "Retrieves dockers images present inside a repository"
  [repo]
  (->> (sh/exec (str  "aws ecr list-images --repository-name " repo))
       walk/keywordize-keys
       :imageIds
       (group-by :imageDigest)
       (map (fn [[k v]] {:imageDigest k :imageTags (map :imageTag v)}))))

(defn search-images
  "Searches images matching REGEX"
  [repo regex]
  (->> (list-images repo)
       (filter (fn [e] (some #(re-find regex %) (:imageTags e))))))

(defn search-complement-images
  "Searches images matching REGEX"
  [repo regex]
  (->> (list-images repo)
       (filter (fn [e] (not-any? #(re-find regex %) (:imageTags e))))))

(defn delete-image-by-digest
  "Deletes an image by its image digest"
  [repo image-digest]
  (sh/exec (str "aws ecr batch-delete-image --repository-name " repo " --image-ids imageDigest=" image-digest)))

(defn get-repositories-level
  "Returns the number of images in each repository"
  [repositories]
  (->> repositories
       (pmap (fn [x] {x (count (list-images x))}))
       (apply merge)))
