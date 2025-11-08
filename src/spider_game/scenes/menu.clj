(ns spider-game.scenes.menu
  (:require [clunk.audio :as audio]
            [clunk.core :as c]
            [clunk.input :as i]
            [clunk.palette :as p]
            [clunk.scene :as scene]
            [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.util :as u]
            [spider-game.sprites.button :as button]
            [clunk.shape :as shape]))

(def bg-color (p/hex->rgba "#A6A6B5"))

(defn on-click-play
  "Transition from this scene to `:level-01` with a ~500ms frame
  fade-out"
  [state e]
  (scene/transition state :level-01 :transition-length 40))

(def title-build-frames 40)
(def title-build-frame-delay 5)
(def title-complete-frames (* title-build-frames title-build-frame-delay))

(defn title
  [window]
  (sprite/animated-sprite
   :animated-title
   (u/window-pos window [0.5 0])
   [256 200]
   :web-title-spritesheet
   [10240 400]
   :offsets [:center :top]
   :animations {:intro {:frames title-build-frames
                        :y-offset 0
                        :frame-delay title-build-frame-delay}
                :finished {:frames 1
                           :y-offset 1
                           :frame-delay 100}}
   :current-animation :intro
   :scale [4 4]
   :extra {:status :not-started}))

(defn blocker
  []
  (sprite/sprite
   :blocker
   [0 0]
   :draw-fn (fn [state b]
              (shape/fill-rect! state [0 0] [1000 250] bg-color))))

(defn subtitle
  [window]
  (sprite/text-sprite :subtitle
                      ;; add 250 in tween
                      (u/window-pos window [0.5 0])
                      "development"
                      :font-size 64
                      :offsets [:center :top]))

(defn sprites
  "The initial list of sprites for this scene"
  [{:keys [window] :as state}]
  [(subtitle window)
   (blocker)
   (title window)
   (-> (button/button-sprite (mapv + (u/window-pos window [0.5 0.7])
                                   [0 500])
                             "Play")
       (i/add-on-click on-click-play))])

(defn draw-menu!
  "Called each frame, draws the current scene to the screen"
  [state]
  (c/draw-background! bg-color)
  (sprite/draw-scene-sprites! state))

(defn check-title
  "a hack, we don't have on-complete-fns for animations or any way of making them run just once, so let's check the sprite and see if it's finished."
  [{:keys [current-scene] :as state}]
  (let [title (first (filter (sprite/has-group :animated-title)
                             (get-in state [:scenes current-scene :sprites])))]
    (cond
      (and (= :not-started (:status title))
           (= 1 (:animation-frame title)))
      (sprite/update-sprites
       state
       (sprite/is-sprite title)
       #(assoc % :status :building))
      
      (and (= :building (:status title))
           (= 0 (:animation-frame title)))
      (-> state
          (assoc :music-source
                 (audio/play! :music :loop? true))
          (sprite/update-sprites
           (sprite/is-sprite title)
           (fn [t]
             (-> t
                 (sprite/set-animation :finished)
                 (assoc :status :finished))))
          (sprite/update-sprites
           (sprite/has-group :subtitle)
           (fn [b]
             (tween/add-tween
              b
              (tween/tween :pos
                           250
                           :update-fn tween/tween-y-fn
                           :step-count 40
                           :easing-fn tween/ease-out-expo))))
          (sprite/update-sprites
           (sprite/has-group :button)
           (fn [b]
             (tween/add-tween
              b
              (tween/tween :pos
                           -500
                           :update-fn tween/tween-y-fn
                           :step-count 40
                           :easing-fn tween/ease-out-expo
                           :initial-delay 80)))))

      :else
      state)))

(defn update-menu
  "Called each frame, update the sprites in the current scene"
  [state]
  (-> state
      sprite/update-state
      check-title
      tween/update-state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites (sprites state)
   :draw-fn draw-menu!
   :update-fn update-menu})
