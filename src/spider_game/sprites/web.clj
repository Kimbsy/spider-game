(ns spider-game.sprites.web
  (:require [clunk.sprite :as sprite]
            [clunk.shape :as shape]
            [clunk.palette :as p]
            [clunk.util :as u]
            [spider-game.common :as common]
            [clojure.math :as math]))

;; @TODO: probably best th come up with a data structure for the
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

(defn except-broken
  [broken threads]
  (remove (fn [[a b]]
            (or (contains? broken a)
                (contains? broken b)))
          threads))

(defn break-web-at
  ;; @TODO: we should just set `size` on the web, then we wont need to pass in state (it's only used for window dimensions)
  [{:keys [window] :as state} {:keys [broken-vertices] :as web} pos]
  (let [[w h] (u/window-size window)
        c (u/center window)
        r-max (* (/ w 2) (math/sqrt 2))
        n-anchors 13
        n-rings 7

        rings-points (map #(points c
                                   n-anchors
                                   (* (inc %) (/ r-max n-rings)))
                          (range n-rings))

        ;; @TODO: insane duplication between this and initialisation
        ;; only difference is this
        nearest (find-nearest-unbroken (apply concat rings-points) broken-vertices pos)
        broken-vertices (conj broken-vertices nearest)

        rings (map #(partition 2 1
                               (take (inc n-anchors)
                                     (cycle %)))
                   rings-points)
        
        ring-threads (->> (apply concat rings)
                          (except-broken broken-vertices))

        radial-threads (->> (mapcat (partial apply map vector)
                                    (partition 2 1 (concat [(repeat n-anchors c)]
                                                           rings-points)))
                            (except-broken broken-vertices))]
    (-> web
        (assoc :broken-vertices broken-vertices)
        (assoc :radial-threads radial-threads)
        (assoc :ring-threads ring-threads))))

(defn fix-web-at
  [{:keys [window] :as state} {:keys [broken-vertices] :as web} pos]
  (let [[w h] (u/window-size window)
        c (u/center window)
        r-max (* (/ w 2) (math/sqrt 2))
        n-anchors 13
        n-rings 7

        rings-points (map #(points c
                                   n-anchors
                                   (* (inc %) (/ r-max n-rings)))
                          (range n-rings))

        ;; only fix if close enough
        max-d 100
        nearest (find-nearest-broken (apply concat rings-points) broken-vertices pos max-d)
        broken-vertices (disj broken-vertices nearest)

        rings (map #(partition 2 1
                               (take (inc n-anchors)
                                     (cycle %)))
                   rings-points)

        ring-threads (->> (apply concat rings)
                          (except-broken broken-vertices))

        radial-threads (->> (mapcat (partial apply map vector)
                                    (partition 2 1 (concat [(repeat n-anchors c)]
                                                           rings-points)))
                            (except-broken broken-vertices))]
    (-> web
        (assoc :broken-vertices broken-vertices)
        (assoc :radial-threads radial-threads)
        (assoc :ring-threads ring-threads))))

(defn web
  [window]
  (let [[w h] (u/window-size window)
        c (u/center window)
        r-max (* (/ w 2) (math/sqrt 2))
        n-anchors 13
        n-rings 7
        
        rings-points (map #(points c
                                   n-anchors
                                   (* (inc %) (/ r-max n-rings)))
                          (range n-rings))

        rings (map #(partition 2 1
                               (take (inc n-anchors)
                                     (cycle %)))
                   rings-points)
      
        ring-threads (apply concat rings)

        radial-threads (mapcat (partial apply map vector)
                               (partition 2 1 (concat [(repeat n-anchors c)]
                                                      rings-points)))]
    (sprite/sprite
     :web
     [0 0]
     :update-fn update-web
     :draw-fn draw-web!
     :extra {:radial-threads radial-threads
             :ring-threads ring-threads
             :broken-vertices #{}})))
