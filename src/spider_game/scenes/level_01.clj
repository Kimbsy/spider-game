(ns spider-game.scenes.level-01
  (:require [clunk.core :as c]
            [clunk.text :as text]))

(defn draw-level-01!
  "Called each frame, draws the current scene to the screen"
  [state]
  (c/draw-background! [0 0 0 0])
  (text/draw-text! state [10 100] "it did work, and is different to the old verison"))

(defn init
  "Initialise this scene"
  [state]
  {:sprites []
   :draw-fn   draw-level-01!
   :update-fn identity
   })
