(ns spider-game.common
  (:require [clunk.palette :as p]
            [clunk.sprite :as sprite]))

(def spider-white (p/hex->rgba "#F2F5EA"))
(def spider-black (p/hex->rgba "#2C363F"))

(def silk-white (assoc (p/hex->rgba "#FFFFFF") 3 0.4))

(def light-green [0.52 1.0 0.78 1.0])
(def dark-green [0.16 0.45 0.45 1.0])

(defn rand-color [] [(rand-int 255) (rand-int 255) (rand-int 255) 1])

(defn draw-level-01!
  [state]
  (let [{:keys [draw-fn sprites]} (get-in state [:scenes :level-01])]
    (doall
     (map (fn [{:keys [draw-fn debug?] :as s}]
            (draw-fn state s)
            (when debug?
              (sprite/draw-bounds state s)
              (sprite/draw-center state s)))
          sprites))))
