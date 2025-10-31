(ns spider-game.common
  (:require [clunk.palette :as p]))

(def spider-white (p/hex->rgba "#F2F5EA"))
(def spider-black (p/hex->rgba "#2C363F"))

(def silk-white (assoc (p/hex->rgba "#FFFFFF") 3 0.4))
