(ns spider-game.sprites.web
  (:require [clunk.sprite :as sprite]
            [clunk.shape :as shape]
            [clunk.palette :as p]
            [clunk.util :as u]
            [spider-game.common :as common]
            [clojure.math :as math]
            [clunk.core :as c]))

;; @TODO: probably best to come up with a data structure for the
;; threads which can contains status, and be used for collision
;; detection.

(defn draw-web!
  [state {:keys [radial-threads ring-threads] :as web}]
  (shape/draw-lines! state radial-threads common/silk-white :line-width 2)
  (shape/draw-lines! state ring-threads common/silk-white))

(defn update-web
  [web]
  web)

(defn points
  "Get `n` points at radius `r` from position `pos`"
  [pos n r]
  (let [vertical [0 r]]
    (for [i (range n)]
      (mapv + pos (u/rotate-vector vertical (* i (/ 360 n)))))))

(defn except-broken
  [broken threads]
  (remove (fn [[a b]]
            (or (contains? broken a)
                (contains? broken b)))
          threads))

(defn recalculate-web
  [{:keys [center broken-vertices r-max n-anchors n-rings initial-points-in-rings] :as web}]
  (let [ring-threads (->> initial-points-in-rings
                          (map #(partition
                                 2 1
                                 (take (inc n-anchors)
                                       (cycle %))))
                          (apply concat)
                          (except-broken broken-vertices))

        radial-threads (->> (mapcat (partial apply map list)
                                    (partition 2 1 (concat [(repeat n-anchors center)]
                                                           initial-points-in-rings)))
                            (except-broken broken-vertices))]
    (-> web
        (assoc :radial-threads radial-threads)
        (assoc :ring-threads ring-threads))))

(defn find-nearest-unbroken
  [points broken pos]
  (:best
   (reduce (fn [{:keys [best best-d] :as acc} p]
             (let [d (u/magnitude (mapv - pos p))]
               (if (and (not (contains? broken p))
                        (< d best-d))
                 (-> acc
                     (assoc :best p)
                     (assoc :best-d d))
                 acc)))
           {:best nil
            :best-d ##Inf}
           points)))

(defn find-nearest-broken
  [points broken pos max-d]
  (:best
   (reduce (fn [{:keys [best best-d] :as acc} p]
             (let [d (u/magnitude (mapv - pos p))]
               (if (and (contains? broken p)
                        (< d best-d))
                 (-> acc
                     (assoc :best p)
                     (assoc :best-d d))
                 acc)))
           {:best nil
            :best-d max-d}
           points)))

(defn thread-set
  [{:keys [radial-threads ring-threads]}]
  (into (set radial-threads) ring-threads))

(defn break-web-at
  [{:keys [broken-vertices initial-points-in-rings radial-threads ring-threads] :as web} pos]
  (let [current-threads (thread-set web)
        nearest (find-nearest-unbroken
                 (apply concat initial-points-in-rings)
                 broken-vertices
                 pos)
        broken-vertices (conj broken-vertices nearest)
        new-web (recalculate-web (assoc web :broken-vertices broken-vertices))]
    (when nearest
      (c/enqueue-event! {:event-type :spawn-web-break
                         :source nearest
                         :threads (filter (fn [[a b]]
                                            (or (= a nearest)
                                                (= b nearest)))
                                          current-threads)}))
    new-web))

(defn complete-fix
  [web pos]
  (-> web
      (update :broken-vertices disj pos)
      recalculate-web))

(defn fix-web-at
  [{:keys [broken-vertices initial-points-in-rings] :as web} pos]
  (let [;; only fix if close enough
        max-distance 100
        nearest (find-nearest-broken
                 (apply concat initial-points-in-rings)
                 broken-vertices
                 pos
                 max-distance)
        broken-vertices (disj broken-vertices nearest)]
    (if nearest
      (let [current-threads (thread-set web)
            broken-vertices (disj broken-vertices nearest)
            new-web (recalculate-web (assoc web :broken-vertices broken-vertices))
            new-threads (thread-set new-web)]
        (c/enqueue-event! {:event-type :spawn-web-fix
                           :source nearest
                           :threads (clojure.set/difference new-threads current-threads)})
        web)
      web)))

(defn points-in-rings
 "Get all of the points, grouped into rings"
  [center r-max n-anchors n-rings]
  (map #(points center
                n-anchors
                (* (inc %) (/ r-max n-rings)))
       (range n-rings)))

(defn web
  [window]
  (let [[w h] (u/window-size window)
        c (u/center window)
        r-max (* (/ w 2) (math/sqrt 2))
        n-anchors 13
        n-rings 7
        initial-points-in-rings (points-in-rings c r-max n-anchors n-rings)]
    (-> (sprite/sprite
         :web
         [0 0]
         :update-fn update-web
         :draw-fn draw-web!
         :extra {:center c
                 :r-max r-max
                 :n-anchors n-anchors
                 :n-rings n-rings
                 :broken-vertices #{}
                 :initial-points-in-rings initial-points-in-rings})
        recalculate-web)))
