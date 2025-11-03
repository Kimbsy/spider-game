(ns spider-game.sprites.fly
  (:require [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.shape :as shape]
            [clunk.palette :as p]))

;; How much of the total (1.0) escape per frame
(def escape-speed 0.001)

(defn draw-fly!
  [state
   {:keys [current-animation remaining hide-timer?]
    [fx fy] :pos
    [fw fh] :size
    :as fly}]
  (sprite/draw-animated-sprite! state fly)

  (when (and (= :escaping current-animation)
             (not hide-timer?))
    (let [bw fw
          bh 10
          bx (- fx (/ bw 2))
          by (+ fy (/ fh 2))]

      (shape/fill-rect! state [bx by] [bw bh] p/grey)
      (when (pos? remaining)
        (shape/fill-rect! state [bx by] [(* bw remaining) bh] p/cyan)))))

(defn update-fly!
  [{:keys [status] :as fly}]
  (-> fly
      ((fn [f]
         (if (= :escaping status)
           (update f :remaining - escape-speed)
           f)))
      sprite/update-animated-sprite))

(defn fly
  [pos &
   {:keys [hide-timer?]
    :or {hide-timer? false}}]
  (-> (sprite/animated-sprite
       :fly
       pos
       [64 96]
       :fly-spritesheet
       [64 192]
       :animations {:escaping {:frames 1
                               :y-offset 0
                               :frame-delay 100}
                    :wrapped {:frames 1
                              :y-offset 1
                              :frame-delay 100}}
       :current-animation :escaping
       :rotation (rand-int 360)
       :draw-fn draw-fly!
       :update-fn update-fly!
       :extra {:remaining (+ 0.5 (/ (rand) 2))
               :hide-timer? hide-timer?
               :status :escaping})
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
