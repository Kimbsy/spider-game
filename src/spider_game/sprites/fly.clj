(ns spider-game.sprites.fly
  (:require [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.shape :as shape]
            [clunk.palette :as p]
            [clunk.core :as c]))

;; How much of the total (1.0) escape per frame
(def escape-speed 0.001)

(defn begin-escape
  [fly]
  (let [half-move-t 35
        move-t (* half-move-t 2)]
    (-> fly
        (assoc :status :escaping)
        (assoc :hide-timer? true)
        (update :tweens (fn [ts] (remove #(= :wiggle (:tag %)) ts)))
        (tween/add-tween
         (tween/tween :pos
                      (rand-nth [150 -150])
                      :update-fn tween/tween-x-fn
                      :step-count move-t))
        (tween/add-tween
         (tween/tween :pos
                      -100
                      :update-fn tween/tween-y-fn
                      :step-count half-move-t
                      :easing-fn tween/ease-out-sine
                      :yoyo? true
                      :yoyo-update-fn tween/tween-y-yoyo-fn))
        (tween/add-tween
         (tween/tween :scale
                      -1
                      :update-fn (fn [[x y] d]
                                   [(+ x d) (+ y d)])
                      :step-count move-t)))))

(defn wrap-fly
  [{:keys [rotation]
    [x y] :pos
    :as f}]
  (-> f
      (sprite/set-animation :wrapped)
      (assoc :status :wrapped)
      ;; @TODO: would be great to have a tween until function to
      ;; recreate x and y tweens to a specific pos
      (tween/add-tween
       (tween/tween :pos
                    60
                    :step-count 30
                    :easing-fn tween/ease-out-expo
                    :from-value x
                    :update-fn tween/tween-x-fn
                    :on-complete-fn (fn [f]
                                      (c/enqueue-event! {:event-type :inc-score
                                                         :fly (assoc f :status :scoring)})
                                      (assoc f :status :remove-me))))
      (tween/add-tween
       (tween/tween :pos
                    50
                    :step-count 30
                    :easing-fn tween/ease-out-expo
                    :from-value y
                    :update-fn tween/tween-y-fn))
      (tween/add-tween
       (tween/tween :rotation
                    25
                    :step-count 30
                    :easing-fn tween/ease-out-expo
                    :from-value rotation))
      (update :tweens (fn [ts] (remove #(= :wiggle (:tag %)) ts)))))

(defn draw-fly!
  [state
   {:keys [current-animation remaining hide-timer?]
    [fx fy] :pos
    [fw fh] :size
    :as fly}]
  (sprite/draw-animated-sprite! state fly)

  ;; draw escape timer
  (when (and (not= :wrapped current-animation)
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
         (if (= :struggling status)
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
       [64 288]
       :animations {:struggling {:frames 1
                                 :y-offset 0
                                 :frame-delay 100}
                    :biteable {:frames 1
                               :y-offset 1
                               :frame-delay 100}
                    :wrapped {:frames 1
                              :y-offset 2
                              :frame-delay 100}}
       :current-animation :struggling
       :rotation (rand-int 360)
       :draw-fn draw-fly!
       :update-fn update-fly!
       :extra {:remaining (+ 0.5 (/ (rand) 2))
               :hide-timer? hide-timer?
               :status :struggling
               :scale [1 1]})
      (tween/add-tween
       (assoc
        (tween/tween
         :rotation
         20
         :step-count 5
         :yoyo? true
         :repeat-times ##Inf
         :initial-delay (rand-int 20)
         :on-repeat-delay 20
         :easing-fn tween/ease-in-out-sine)
        :tag :wiggle))))
