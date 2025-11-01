(ns spider-game.sprites.fly
  (:require [clunk.sprite :as sprite]
            [clunk.tween :as tween]))

(defn fly
  [pos]
  (-> (sprite/animated-sprite
       :fly
       pos
       [64 96]
       :fly-spritesheet
       [64 96]
       :animations {:none {:frames 1
                           :y-offset 0
                           :frame-delay 100}}
       :current-animation :none
       :rotation (rand-int 360))
      (tween/add-tween
       (tween/tween
        :rotation
        20
        :step-count 5
        :yoyo? true
        :repeat-times ##Inf
        :initial-delay (rand-int 20)
        :on-repeat-delay 20
        :easing-fn tween/ease-in-out-sine
        ))))
