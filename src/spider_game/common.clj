(ns spider-game.common
  (:require [clunk.palette :as p]))

(def spider-white (p/hex->rgba "#F2F5EA"))
(def spider-black (p/hex->rgba "#2C363F"))

(def silk-white (assoc (p/hex->rgba "#FFFFFF") 3 0.4))

(def light-green [0.52 1.0 0.78 1.0])
(def dark-green [0.16 0.45 0.45 1.0])

(defn rand-color [] [(rand-int 255) (rand-int 255) (rand-int 255) 1])
