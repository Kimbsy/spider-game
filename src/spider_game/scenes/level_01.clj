(ns spider-game.scenes.level-01
  (:require [clunk.core :as c]
            [clunk.input :as i]
            [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.util :as u]
            [spider-game.sprites.player-spider :as ps]
            [spider-game.sprites.web :as web]))

(defn f [c] (mapv #(float (/  % 255)) c))

(def light-green [0.52 1.0 0.78 1.0])
(def dark-green [0.16 0.45 0.45 1.0])
(defn rand-color [] [(rand-int 255) (rand-int 255) (rand-int 255) 1])

(defn sprites
  "The initial list of sprites for this scene"
  [{:keys [window] :as state}]
  [(web/web window)
   (ps/spider (u/center window))])

(defn draw-level-01!
  "Called each frame, draws the current scene to the screen"
  [state]
  (c/draw-background! dark-green)
  (sprite/draw-scene-sprites! state))

(defn update-level-01
  "Called each frame, update the sprites in the current scene"
  [state]
  (-> state
      sprite/update-state
      tween/update-state))

(defn clicked
  [state {:keys [pos] :as e}]
  (cond
    (i/is e :button i/M_RIGHT)
    (sprite/update-sprites
     state
     (sprite/has-group :player-spider)
     #(ps/move % pos))

    :else
    state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites   (sprites state)
   :draw-fn   draw-level-01!
   :update-fn update-level-01
   :mouse-button-fns [clicked]})
