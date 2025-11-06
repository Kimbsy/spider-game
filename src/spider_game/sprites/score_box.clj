(ns spider-game.sprites.score-box
  (:require [clunk.sprite :as sprite]
            [clunk.text :as text]
            [clunk.shape :as shape]
            [clunk.palette :as p]
            [clunk.tween :as tween]))

(defn draw-score-box!
  [state
   {:keys [pos show-fly? fly score]
    [w h :as size] :size}]
  (shape/fill-rect! state
                    (map - pos [(/ w 2) (/ h 2)])
                    size
                    p/grey)

  ;; draw fly
  (when show-fly?
    (sprite/draw-animated-sprite! state (assoc fly :pos pos)))

  ;; draw score
  (when (< 1 score)
    (let [content (str "x" score)]
      (text/draw-text! state
                       (map + pos
                            size
                            [(- (* (count content) 16))
                             -2]
                            [(- (/ w 2))
                             (- (/ h 2))])
                       content))))

(defn hide
  [{:keys [hidden-y]
    [x y] :pos
    :as sb}]
  (tween/add-tween
   sb
   (tween/tween :pos
                hidden-y
                :from-value y
                :step-count 20
                :update-fn tween/tween-y-fn
                :easing-fn tween/ease-out-expo
                :initial-delay 50)))

(defn show
  [{:keys [visible-y]
    [x y] :pos
    :as sb}]
  (tween/add-tween
   sb
   (tween/tween :pos
                visible-y
                :from-value y
                :step-count 20
                :update-fn tween/tween-y-fn
                :easing-fn tween/ease-out-expo
                :on-complete-fn hide)))

(defn score-box
  []
  (sprite/sprite
   :score-box
   [60 -75]
   :size [100 100]
   :draw-fn draw-score-box!
   :extra {:visible-y 50
           :hidden-y -75
           :score 0
           :status :hidden
           ;; this will be the first fly we capture
           :fly nil
           :show-fly? false}))
