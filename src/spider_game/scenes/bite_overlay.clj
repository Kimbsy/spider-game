(ns spider-game.scenes.bite-overlay
  (:require [clunk.collision :as collision]
            [clunk.core :as c]
            [clunk.input :as i]
            [clunk.palette :as p]
            [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.util :as u]
            [spider-game.common :as common]
            [clunk.scene :as scene]
            [spider-game.scenes.wrap-overlay :as wrap-overlay]))

(defn sprites
  [{:keys [window] :as state}]
  (let [[w h] (u/window-size window)
        [cx cy :as center] (u/center window)
        shadow-pad 15
        hw-pad 100
        c-pad 10
        c-h 200
        bgw (- w (* hw-pad 2))
        bgh c-h
        fgw (- bgw c-pad)
        fgh (- bgh c-pad)
        iw (- fgw c-pad)
        ih (- fgh c-pad)]
    (concat
     [(sprite/geometry-sprite :shadow
                              [(+ cx shadow-pad) (+ cy shadow-pad)]
                              [[0 0]
                               [0 bgh]
                               [bgw bgh]
                               [bgw 0]]
                              :size [bgw bgh]
                              :color (assoc p/black 3 0.4)
                              :fill? true)
      (sprite/geometry-sprite :bg-container
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
                              :fill? true)]
     (let [tw (- fgw 50)
           th 30
           gw (/ tw 5)
           gh (* th 1.3)
           gx (- (+ cx (rand-int (- tw gw))) (/ (- tw gw) 2))
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
                                 :color common/spider-white
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
                (assoc
                 (tween/tween
                  :pos
                  tw
                  :update-fn tween/tween-x-fn
                  :yoyo? true
                  :yoyo-update-fn tween/tween-x-yoyo-fn
                  :repeat-times ##Inf)
                 :tag :movement)))]))))))

(defn draw-bite-overlay!
  [{:keys [window] :as state}]
  ;; draw level-01 scene
  (c/draw-background! common/dark-green)
  (common/draw-level-01! state)

  (sprite/draw-scene-sprites! state))

(defn update-bite-overlay
  [state]
  (-> state
      tween/update-state))

(defn attempt-bite
  [{:keys [current-scene] :as state} e]
  (if (i/is e :key i/K_SPACE)
    (let [sprites (get-in state [:scenes current-scene :sprites])
          cb (first (filter (sprite/has-group :current-bite) sprites))
          g (first (filter (sprite/has-group :good) sprites))
          p (first (filter (sprite/has-group :perfect) sprites))]
      (cond
        ;; PERFECT
        (collision/w-h-rects-collide? cb p)
        (-> state
            ((fn [state]
               (assoc-in state
                         [:scenes :wrap-overlay]
                         ;; also insert he fly uuid so we know
                         ;; which sprite to wrap once we're done
                         (assoc (wrap-overlay/init state)
                                :fly-uuid (get-in state [:scenes current-scene :fly-uuid])))))
            (scene/transition
             :wrap-overlay))

        ;; GOOD
        (collision/w-h-rects-collide? cb g)
        (-> state
            ((fn [state]
               (assoc-in state
                         [:scenes :wrap-overlay]
                         ;; also insert he fly uuid so we know
                         ;; which sprite to wrap once we're done
                         (assoc (wrap-overlay/init state)
                                :fly-uuid (get-in state [:scenes current-scene :fly-uuid])))))
            (scene/transition
             :wrap-overlay))

        ;; else 
        :else
        (do
          (sprite/update-sprites
           state
           (sprite/has-group :current-bite)
           (fn [s]
             (let [timeout 40]
               (-> s
                   (update :tweens
                           (fn [ts]
                             (map (fn [t]
                                    (if (= :movement (:tag t))
                                      (assoc t :delay timeout)
                                      t))
                                  ts)))
                   (tween/add-tween
                    (tween/tween
                     :pos
                     20
                     :step-count 4
                     :update-fn tween/tween-y-fn
                     :yoyo? true
                     :yoyo-update-fn tween/tween-y-yoyo-fn
                     :repeat-times 5)))))))))
    state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites (sprites state)
   :draw-fn draw-bite-overlay!
   :update-fn update-bite-overlay
   :key-fns [attempt-bite]})
