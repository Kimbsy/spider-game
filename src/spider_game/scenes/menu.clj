(ns spider-game.scenes.menu
  (:require [clunk.core :as c]
            [clunk.input :as input]
            [clunk.palette :as p]
            [clunk.scene :as scene]
            [clunk.sprite :as sprite]
            [clunk.util :as u]
            [spider-game.sprites.button :as button]
            [clunk.tween :as tween]))

(def grey (p/hex->rgba "#A6A6B5"))

(defn on-click-play
  "Transition from this scene to `:level-01` with a ~500ms frame
  fade-out"
  [state e]
  (scene/transition state :level-01 :transition-length 40))

(defn title
  [window]
  (sprite/text-sprite :title
                      (u/window-pos window [0.5 0.15])
                      "WEB 4.0"
                      :font-size 128))

(defn caption
  [window]
  (-> (sprite/text-sprite :title
                       (u/window-pos window [0.8 0.37])
                       "(the bugs are a feature)"
                       :color p/yellow
                       :font-size 24
                       :rotation -25)
      (tween/add-tween
       (tween/tween :pos
                    -10
                    :step-count 40
                    :easing-fn tween/ease-in-out-sine
                    :yoyo? true
                    :update-fn tween/tween-y-fn
                    :yoyo-update-fn tween/tween-y-yoyo-fn
                    :repeat-times ##Inf))))

(defn sprites
  "The initial list of sprites for this scene"
  [{:keys [window] :as state}]
  [(title window)
   (caption window)
   (-> (button/button-sprite (u/window-pos window [0.5 0.7]) "Play")
       (input/add-on-click on-click-play))])

(defn draw-menu!
  "Called each frame, draws the current scene to the screen"
  [state]
  (c/draw-background! grey)
  (sprite/draw-scene-sprites! state))

(defn update-menu
  "Called each frame, update the sprites in the current scene"
  [state]
  (-> state
      sprite/update-state
      tween/update-state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites (sprites state)
   :draw-fn draw-menu!
   :update-fn update-menu})
