(ns spider-game.scenes.wrap-overlay
  (:require [clunk.util :as u]
            [clunk.sprite :as sprite]
            [spider-game.common :as common]
            [clunk.palette :as p]
            [clunk.core :as c]
            [clunk.tween :as tween]
            [clunk.input :as i]
            [clunk.scene :as scene]
            [spider-game.sprites.fly :as fly]))

(defn sprites
  [{:keys [window] :as state}]
  (let [[w h] (u/window-size window)
        [cx cy :as center] (u/center window)
        hw-pad 200
        vw-pad 200
        c-pad 10
        bgw (- w (* hw-pad 2))
        bgh (- h (* vw-pad 2))
        fgw (- bgw c-pad)
        fgh (- bgh c-pad)
        iw (- fgw c-pad)
        ih (- fgh c-pad)]
    (concat
     [(sprite/geometry-sprite :bg-container
                              center
                              [[0 0]
                               [0 bgh]
                               [bgw bgh]
                               [bgw 0]]
                              :size [bgw bgh]
                              :color p/grey
                              :fill? true)
      (sprite/geometry-sprite :container
                              center
                              [[0 0]
                               [0 fgh]
                               [fgw fgh]
                               [fgw 0]]
                              :size [fgw fgh]
                              :color common/spider-white
                              :fill? true)
      (sprite/geometry-sprite :inner
                              center
                              [[0 0]
                               [0 ih]
                               [iw ih]
                               [iw 0]]
                              :size [iw ih]
                              :color p/grey
                              :fill? true)
      (fly/fly center)])))

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

(defn draw-wrap-overlay!
  [{:keys [window] :as state}]
  ;; draw level-01 scene
  (c/draw-background! common/dark-green)
  (draw-level-01! state)

  (sprite/draw-scene-sprites! state))

(defn update-wrap-overlay
  [state]
  (-> state
      tween/update-state))

(defn esc
  [state e]
  (if (i/is e :key i/K_ESCAPE)
    (scene/transition state :level-01 :transition-length 0)
    state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites (sprites state)
   :draw-fn draw-wrap-overlay!
   :update-fn update-wrap-overlay
   :mouse-button-fns []
   :key-fns [esc]})
