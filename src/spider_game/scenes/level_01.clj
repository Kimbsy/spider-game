(ns spider-game.scenes.level-01
  (:require [clunk.core :as c]
            [clunk.collision :as collision]
            [clunk.input :as i]
            [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.util :as u]
            [spider-game.sprites.player-spider :as ps]
            [spider-game.sprites.web :as web]
            [spider-game.sprites.fly :as fly]
            [clunk.audio :as audio]
            [clunk.scene :as scene]))

(defn f [c] (mapv #(float (/  % 255)) c))

(def light-green [0.52 1.0 0.78 1.0])
(def dark-green [0.16 0.45 0.45 1.0])
(defn rand-color [] [(rand-int 255) (rand-int 255) (rand-int 255) 1])

(defn flies
  [n window]
  (repeatedly n #(fly/fly (u/window-pos window [(rand) (rand)]))))

(defn sprites
  "The initial list of sprites for this scene"
  [{:keys [window] :as state}]
  (concat 
   [(web/web window)]
   (flies 10 window)
   [(ps/spider (u/center window))]))

(defn draw-level-01!
  "Called each frame, draws the current scene to the screen"
  [state]
  (c/draw-background! dark-green)
  (sprite/draw-scene-sprites! state))

(defn remove-dead
  [{:keys [current-scene] :as state}]
  (update-in state [:scenes current-scene :sprites]
             #(remove :dead %)))

(defn update-level-01
  "Called each frame, update the sprites in the current scene"
  [state]
  (-> state
      remove-dead
      sprite/update-state
      tween/update-state
      ((fn [state]
         (sprite/update-sprites
          state
          (sprite/has-group :player-spider)
          (fn [s]
            (assoc s :debug? false)))))
      collision/update-state
      ))

;; @TODO: not working, could do with a "no collisions fn", otherwise we trigger non-hit for each fly that doesn't collide
(defn colliders
  []
  [(collision/collider
    :player-spider
    :fly
    (fn [spider _fly]
      (assoc spider :debug? true))
    collision/identity-collide-fn)])

(defn clicked
  [state {:keys [pos] :as e}]
  (cond
    (i/is e :button i/M_RIGHT)
    (sprite/update-sprites
     state
     (sprite/has-group :player-spider)
     #(ps/move % pos))

    :else
    state))

(defn bite
  [{:keys [current-scene] :as state} e]
  (if (i/is e :key i/K_SPACE)
    (scene/transition state :bite-overlay)
    #_(let [s (first (filter (sprite/has-group :player-spider)
                           (get-in state [:scenes current-scene :sprites])))]
      (update-in state
                 [:scenes current-scene :sprites]
                 (fn [sprites]
                   (if-let [food (first
                                  (filter #(and (= :fly (:sprite-group %1))
                                                (collision/w-h-rects-collide? %1 s))
                                          sprites))]
                     (map (fn [f]
                            (if (= (:uuid f) (:uuid food))
                              (do (audio/play! :munch)
                                  (assoc f :dead true))
                              f))
                          sprites)
                     sprites))))
    state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites   (sprites state)
   :draw-fn   draw-level-01!
   :update-fn update-level-01
   :colliders (colliders)
   :mouse-button-fns [clicked]
   :key-fns [bite]
   })
