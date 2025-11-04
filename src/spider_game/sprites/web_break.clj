(ns spider-game.sprites.web-break
  (:require [clunk.sprite :as sprite]
            [clunk.shape :as shape]
            [clunk.palette :as p]
            [spider-game.common :as common]))

(def break-frames 40)

(defn draw-web-break!
  [state {:keys [threads progress]}]
  (let [alpha (- 1 (float (/ progress break-frames)))
        lines (map :line threads)]
    (shape/draw-lines! state lines (update common/silk-white 3 * alpha))))

(defn update-thread
  [{:keys [avel bvel] :as thread}]
  (update thread :line (fn [[a b]]
                          [(mapv + avel a)
                           (mapv + bvel b)])))

(defn update-web-break
  [{:keys [progress] :as wb}]
  (if (<= break-frames progress)
    (assoc wb :status :remove-me)
    (-> wb
        (update :progress inc)
        (update :threads #(map update-thread %)))))

(defn prep-thread
  [thread]
  {:line thread
   :avel [(dec (rand 2)) (dec (rand 2))]
   :bvel [(dec (rand 2)) (dec (rand 2))]})

(defn web-break
  [source threads]
  (sprite/sprite
   :web-break
   source
   :update-fn update-web-break
   :draw-fn draw-web-break!
   :extra {:source source
           :threads (map prep-thread threads)
           :progress 0}))
