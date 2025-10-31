(ns spider-game.scenes.level-01
  (:require [clunk.audio :as audio]
            [clunk.core :as c]
            [clunk.input :as i]
            [clunk.palette :as p]
            [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.util :as u]
            [clunk.shape :as shape]))

(defn f [c] (mapv #(float (/  % 255)) c))

(def light-green [0.52 1.0 0.78 1.0])
(def dark-green [0.16 0.45 0.45 1.0])
(defn rand-color [] [(rand-int 255) (rand-int 255) (rand-int 255) 1])

(def leg-color (p/hex->rgba "#F2F5EA"))
(def foot-color (p/hex->rgba "#F2F5EA"))
(def body-color (p/hex->rgba "#F2F5EA"))
(def eye-color (p/hex->rgba "#2C363F"))

(def min-length 50)
(def max-length 160)
(def max-angle 45)

(def ls 100)
(def lt 60)

(def move-foot-time 5)

(def stopped-time-limit 40)

(def step-sounds [:step-1 :step-2 :step-3 :step-4 :step-5])

(defn move-foot-tween-x
  [idx [fx _] [tx _]]
  (tween/tween :feet
               tx
               :from-value fx
               :step-count move-foot-time
               :update-fn (fn [feet d]
                            (update-in feet
                                       [idx :pos 0]
                                       + d))
               :on-complete-fn (fn [spider]
                                 (audio/play! (rand-nth step-sounds))
                                 (update spider :moving-feet
                                         assoc idx false))))

(defn move-foot-tween-y
  [idx [_ fy] [_ ty]]
  (tween/tween :feet
               ty
               :from-value fy
               :step-count move-foot-time
               :update-fn (fn [feet d]
                            (update-in feet
                                       [idx :pos 1]
                                       + d))
               ;; @NOTE don't need the on-complete for Y, since X
               ;; already does it.
               ))

(defn length
  [[ax ay] [bx by]]
  (Math/sqrt (+ (Math/pow (- ax bx) 2)
                (Math/pow (- ay by) 2))))

(defn should-move?
  [spider-pos foot-pos reset-pos]
  (let [leg-length (length spider-pos foot-pos)]
    (or (< leg-length min-length)
        (< max-length leg-length)
        (< max-angle
           (abs (- (u/rotation-angle (map - foot-pos spider-pos))
                   (u/rotation-angle (map - reset-pos spider-pos))))))))

(defn reset-feet
  [{:keys [pos feet] :as spider}]
  (-> (reduce (fn [acc-spider [i {:keys [reset-offset] :as foot}]]
                (-> acc-spider
                    (tween/add-tween
                     (assoc
                      (move-foot-tween-x
                       i
                       (:pos foot)
                       (map + pos reset-offset))
                      :initial-delay (* i 3)
                      :tag :reset-feet))
                    (tween/add-tween
                     (assoc
                      (move-foot-tween-y
                       i
                       (:pos foot)
                       (map + pos reset-offset))
                      :initial-delay (* i 3)
                      :tag :reset-feet))))
              spider
              (zipmap (range) feet))
      (assoc :status :idle)
      (assoc :stopped-time 0)))

(defn update-spider
  [{:keys [feet pos moving-feet stopped-time status] :as spider}]
  (let [movements (keep-indexed
                   (fn [i {:keys [reset-offset]
                           foot-pos :pos
                           :as foot}]
                     (let [reset-pos (map + pos reset-offset)]
                       (when (and (not (moving-feet i))
                                  (should-move? pos foot-pos reset-pos))
                         ;; @TODO: this can be simplified
                         (let [target-pos (map +
                                               foot-pos
                                               (map #(* 1.5 %)
                                                    (map - reset-pos foot-pos)))]
                           [i [(move-foot-tween-x i foot-pos target-pos)
                               (move-foot-tween-y i foot-pos target-pos)]]))))
                   feet)
        applied-movements (reduce (fn [acc-spider [i [tween-x tween-y]]]
                                    (-> acc-spider
                                        (tween/add-tween tween-x)
                                        (tween/add-tween tween-y)
                                        (update :moving-feet assoc i true)))
                                  spider
                                  movements)]
    (cond
      (< stopped-time-limit stopped-time)
      (reset-feet applied-movements)

      (= :stopped status)
      (update applied-movements :stopped-time inc)

      :else
      applied-movements)))

(defn leg-lines
  [ls lt [fx fy :as f] [bx by :as b] k1?]
  (let [l (length f b)

        ;; d is the unit vector from f to b
        [dx dy :as d] [(/ (- bx fx) l)
                       (/ (- by fy) l)]

        ;; a is the distance form f to the intersection midpoint
        a (/ (+ (* ls ls)
                (- (* lt lt))
                (* l l))
             (* l 2))

        ;; p is the point along the line f->b at the intersection point
        [px py :as p] (mapv + f (mapv #(* a %) d))

        ;; perp is perpendicular to p
        perp [(- dy) dx]

        ;; h is height of k above p
        h (Math/sqrt (- (* ls ls)
                        (* a a)))

        ;; kn are the points for the knee
        [k1x k1y :as k1] (mapv + p (mapv #(* h %) perp))
        [k2x k2y :as k2] (mapv - p (mapv #(* h %) perp))]

    (if k1?
      [[[bx by]
        [k1x k1y]]
       [[k1x k1y]
        [fx fy]]]
      [[[bx by]
        [k2x k2y]]
       [[k2x k2y]
        [fx fy]]])))

(defn draw-spider
  [state
   {[x y :as pos] :pos
    :keys [feet]}]

  (let [lines (mapcat (fn [[i {[fx fy :as f-pos] :pos}]]
                        (leg-lines ls lt f-pos pos (< i 4)))
                      (zipmap (range) feet))]
    (shape/draw-lines! state lines leg-color :line-width 4))

  ;; draw body
  (shape/fill-ellipse! state (mapv + pos [-15 -25]) [30 30] body-color)

  (shape/fill-rects!
   state
   [[[(- x 5) (- y 10)] [2 4]]
    [[(+ x 5) (- y 10)] [2 4]]]
   eye-color))

(defn spider
  [pos]
  (let [initial-offsets [[100 0]
                         [100 25]
                         [80 50]
                         [60 75]
                         [-100 0]
                         [-100 25]
                         [-80 50]
                         [-60 75]]
        s (sprite/sprite
           :spider
           pos
           :update-fn update-spider
           :draw-fn draw-spider
           :extra {:initial-offsets initial-offsets
                   :feet (mapv (fn [offset]
                                 {:reset-offset offset
                                  :pos (mapv + pos offset)})
                               initial-offsets)
                   :moving-feet {}
                   :stopped-time 0
                   :status :idle})]
    (tween/add-tween
     s
     (tween/tween
      :pos
      7
      :step-count (* move-foot-time 10)
      :easing-fn tween/ease-in-out-sine
      :update-fn tween/tween-y-fn
      :yoyo? true
      :yoyo-update-fn tween/tween-y-yoyo-fn
      :repeat-times ##Inf))))

(defn sprites
  "The initial list of sprites for this scene"
  [{:keys [window] :as state}]
  [(spider (u/center window))])

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
  [state {[x y] :pos :as e}]
  (cond
    (i/is e :button i/M_RIGHT)
    (sprite/update-sprites
     state
     (sprite/has-group :spider)
     (fn [spider]
       (-> spider
           (assoc :status :moving)
           (update :tweens (fn [tweens]
                             (remove #(#{:current-move :reset-feet} (:tag %))
                                     tweens)))
           (tween/add-tween
            (assoc (tween/tween
                    :pos
                    x
                    :from-value (get-in spider [:pos 0])
                    :update-fn tween/tween-x-fn
                    :easing-fn tween/ease-out-quad
                    :on-complete-fn (fn [s]
                                      (-> s
                                          (assoc :status :stopped)
                                          (assoc :idle-time 0))))
                   :tag :current-move))
           (tween/add-tween
            (assoc (tween/tween
                    :pos
                    y
                    :from-value (get-in spider [:pos 1])
                    :update-fn tween/tween-y-fn
                    :easing-fn tween/ease-out-quad)
                   :tag :current-move)))))

    :else
    state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites   (sprites state)
   :draw-fn   draw-level-01!
   :update-fn update-level-01
   :mouse-button-fns [clicked]})
