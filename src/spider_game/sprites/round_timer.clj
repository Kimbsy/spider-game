(ns spider-game.sprites.round-timer
  (:require [clunk.sprite :as sprite]
            [clunk.palette :as p]))

(defn update-state
  [{:keys [current-scene dt] :as state}]
  (sprite/update-sprites
   state
   (sprite/has-group :timer)
   (fn [{:keys [remaining-ms] :as t}]
     (let [new (max 0 (- remaining-ms dt))]
       (-> t
           (assoc :remaining-ms new)
           (assoc :content (str (format "%.2f" (float (/ new 1000))) "s")))))))

(defn round-timer
  [[x y] color]
  (sprite/text-sprite :timer
                      [(+ 48 x) y]
                      "60.000s"
                      :font-size 96
                      :color color
                      :extra {:remaining-ms 60000}))
