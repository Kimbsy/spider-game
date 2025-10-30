(ns spider-game.scenes.level-01
  (:require [clunk.text :as text]))

(defn draw-level-01!
  [state]
  (text/draw-text! state [100 100] "Tell him it didn't work"))

(defn init
  "Initialise this scene"
  [state]
  {:sprites   []
   :draw-fn   draw-level-01!
   :update-fn identity})
