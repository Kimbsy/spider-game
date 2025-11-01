(ns spider-game.scenes.bite-overlay
  (:require [clunk.sprite :as sprite]
            [clunk.text :as text]
            [clunk.shape :as shape]
            [spider-game.common :as common]
            [clunk.core :as c]
            [spider-game.scenes.level-01 :as level-01]
            [clunk.palette :as p]
            [clunk.util :as u]
            [clunk.tween :as tween]))

#_(let [[w h] (u/window-size window)
      h-pad 100
      height 200]
  (shape/fill-rect! state
                    [h-pad
                     (- (/ h 2) (/ height 2))]
                    [(- w (* h-pad 2))
                     height]
                    p/grey))

(defn sprites
  [{:keys [window] :as state}]
  (let [[w h] (u/window-size window)
        [cx cy :as center] (u/center window)
        hw-pad 100
        c-pad 10
        c-h 200
        bgw (- w (* hw-pad 2))
        bgh c-h
        fgw (- bgw c-pad)
        fgh (- bgh c-pad)]
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
                              :fill? true)]
     (let [tw (- fgw 50)
           th 30
           gw (/ tw 5)
           gh (* th 1.3)
           gx (- (+ cx (rand-int (- tw gw))) (/ tw 2))
           pw (/ gw 3)
           ph (* gh 1.3)]
       (concat
        [(sprite/geometry-sprite :track
                                 center
                                 [[0 0]
                                  [0 th]
                                  [tw th]
                                  [tw 0]]
                                 :size [tw th]
                                 :color p/grey
                                 :fill? true)
         (sprite/geometry-sprite :good
                                 [gx cy]
                                 [[0 0]
                                  [0 gh]
                                  [gw gh]
                                  [gw 0]]
                                 :size [gw gh]
                                 :color (p/darken p/green)
                                 :fill? true)
         (sprite/geometry-sprite :perfect
                                 [gx cy]
                                 [[0 0]
                                  [0 ph]
                                  [pw ph]
                                  [pw 0]]
                                 :size [pw ph]
                                 :color p/green
                                 :fill? true)]
        (let [cbw 10
              cbh (* ph 1.3)]
          [(-> (sprite/geometry-sprite :current-bite
                                       [(- cx (/ tw 2)) cy]
                                       [[0 0]
                                        [0 cbh]
                                        [cbw cbh]
                                        [cbw 0]]
                                       :size [cbw cbh]
                                       :color p/red
                                       :fill? true)
               (tween/add-tween
                (tween/tween
                 :pos
                 tw
                 :update-fn tween/tween-x-fn
                 :yoyo? true
                 :yoyo-update-fn tween/tween-x-yoyo-fn
                 :repeat-times ##Inf)))]))))))

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

(defn draw-bite-overlay!
  [{:keys [window] :as state}]
  ;; draw level-01 scene
  (c/draw-background! level-01/dark-green)
  (draw-level-01! state)

  (sprite/draw-scene-sprites! state))

(defn update-bite-overlay
  [state]
  (-> state
      tween/update-state))

;; @looks like this scene will process the event that took us here too!?
(defn attempt-bite
  [state e]
  state)

(defn init
  "Initialise this scene"
  [state]
  {:sprites   (sprites state)
   :draw-fn   draw-bite-overlay!
   :update-fn update-bite-overlay
   :key-fns [attempt-bite]})
