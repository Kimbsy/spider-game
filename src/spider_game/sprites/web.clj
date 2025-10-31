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

(defn web
  [window]
  (let [[w h] (u/window-size window)
        c (u/center window)
        r-max (* (/ w 2) (math/sqrt 2))
        n-anchors 13
        radial-threads (map (partial vector c) (points c n-anchors r-max))
        n-rings 7
        ring-threads (mapcat #(partition 2 1
                                         (take (inc n-anchors)
                                               (cycle (points c
                                                              n-anchors
                                                              (* (inc %) (/ r-max n-rings))))))
                             (range n-rings))]
    (sprite/sprite
     :web
     [0 0]
     :update-fn update-web
     :draw-fn draw-web!
     :extra {:radial-threads radial-threads
             :ring-threads ring-threads})))
