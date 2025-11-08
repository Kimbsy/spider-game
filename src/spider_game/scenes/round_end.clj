(ns spider-game.scenes.round-end
  (:require [clunk.core :as c]
            [clunk.palette :as p]
            [clunk.sprite :as sprite]
            [clunk.util :as u]
            [clunk.scene :as scene]
            [clunk.input :as i]
            [spider-game.sprites.button :as button]
            [spider-game.sprites.fly :as fly]
            [clunk.delay :as delay]
            [clunk.tween :as tween]
            [clunk.shape :as shape]
            [clunk.audio :as audio]))

(defn on-click-menu
  [{:keys [restart-fn music] :as state} e]
  (audio/stop! music)
  (scene/transition state
                    :menu
                    :transition-length 40
                    :init-fn restart-fn))

(defn sprites
  [{:keys [window] :as state}]
  [(sprite/text-sprite :flies-caught
                       (u/window-pos window [-0.5 0.3])
                       "Flies caught:"
                       :font-size 48
                       :offsets [:right])
   (sprite/text-sprite :perfect-bites
                       (u/window-pos window [-0.5 0.4])
                       "Perfect bites:"
                       :font-size 48
                       :offsets [:right])
   (sprite/text-sprite :spider-happiness
                       (u/window-pos window [-0.5 0.5])
                       "Spider happiness:"
                       :font-size 48
                       :offsets [:right])
   (sprite/text-sprite :total-score
                       (u/window-pos window [-0.5 0.6])
                       "Total score:"
                       :font-size 48
                       :offsets [:right])
   (-> (button/button-sprite (u/window-pos window [0.5 1.2]) "Menu")
       (i/add-on-click on-click-menu))])

(defn draw-round-end!
  [{:keys [window] :as state}]
  (c/draw-background! p/grey)

  ;; alignment center line
  #_(shape/draw-line! state
                   (u/window-pos window [0.5 0])
                   (u/window-pos window [0.5 1])
                   p/red)
  
  (sprite/draw-scene-sprites! state))

(defn update-round-end
  [state]
  (-> state
      delay/update-state
      tween/update-state))

(defn tween-text-in
  [state sprite-group final-x]
  (sprite/update-sprites
   state
   (sprite/has-group sprite-group)
   (fn [{[x y] :pos :as s}]
     (tween/add-tween
      s
      (tween/tween :pos
                   475
                   :from-value x
                   :step-count 30
                   :easing-fn tween/ease-out-quint
                   :update-fn tween/tween-x-fn)))))

(defn show-flies-caught-text
  [state]
  (tween-text-in state :flies-caught 340))

(defn show-flies
  [{:keys [window current-scene score] :as state}]
  (update-in state [:scenes current-scene :sprites]
             concat
             (for [i (range (:flies-caught score))]
               (-> (fly/fly (u/window-pos window [(+ 0.55 (* 0.04 i)) 0.31]))
                   (assoc :rotation 25)
                   (sprite/set-animation :wrapped)))))

(defn show-perfect-bites-text
  [state]
  (tween-text-in state :perfect-bites 328))

(defn show-perfect-bites
  [{:keys [window current-scene score] :as state}]
  (update-in state [:scenes current-scene :sprites]
             conj (sprite/text-sprite :perfect-bites-out
                                      (u/window-pos window [0.525 0.4])
                                      (str (:perfect-bites score))
                                      :font-size 48
                                      :offsets [:left])))

(defn show-spider-hapiness-text
  [state]
  (tween-text-in state :spider-happiness 291))

(defn show-spider-hapiness
  [{:keys [window current-scene score] :as state}]
  (update-in state [:scenes current-scene :sprites]
             conj (sprite/text-sprite :perfect-bites-out
                                      (u/window-pos window [0.525 0.5])
                                      (:spider-happiness score)
                                      :font-size 48
                                      :offsets [:left])))

(defn show-total-text
  [state]
  (tween-text-in state :total-score 350))

(defn show-total
  [{:keys [window current-scene]
    {:keys [flies-caught perfect-bites spider-happiness]} :score
    :as state}]
  (let [total (* (+ (* 111 flies-caught)
                    (* 73 perfect-bites))
                 (case spider-happiness
                   "very high" 31
                   "high" 23
                   "moderate" 11
                   "low" 5))]
    (update-in state [:scenes current-scene :sprites]
               conj (sprite/text-sprite :total-out
                                        (u/window-pos window [0.525 0.6])
                                        (str total)
                                        :font-size 48
                                        :offsets [:left]))))

(defn show-menu-button
  [state]
  (sprite/update-sprites
   state
   (sprite/has-group :button)
   (fn [{[x y] :pos :as s}]
     (tween/add-tween
      s
      (tween/tween :pos
                   640.0
                   :from-value y
                   :step-count 30
                   :easing-fn tween/ease-out-quint
                   :update-fn tween/tween-y-fn)))))

(defn delay-fns
  [state]
  (delay/sequential-delay-fns
   [[500 show-flies-caught-text]
    [500 show-flies]
    [500 show-perfect-bites-text]
    [500 show-perfect-bites]
    [500 show-spider-hapiness-text]
    [500 show-spider-hapiness]
    [500 show-total-text]
    [500 show-total]
    [500 show-menu-button]]))

(defn init
  "Initialise this scene"
  [state]
  {:sprites (sprites state)
   :draw-fn draw-round-end!
   :update-fn update-round-end
   :delay-fns (delay-fns state)})
